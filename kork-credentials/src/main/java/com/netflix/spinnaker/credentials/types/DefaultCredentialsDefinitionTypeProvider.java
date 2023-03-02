/*
 * Copyright 2023 Apple Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.credentials.types;

import com.netflix.spinnaker.credentials.CredentialsTypes;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionTypeProvider;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.plugins.SpinnakerPluginManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Exports all discovered account definition types from scanning the classpath. Plugins may register
 * additional provider beans to register additional account types to support in {@link
 * CredentialsDefinitionRepository}.
 */
@Component
@NonnullByDefault
@Log4j2
public class DefaultCredentialsDefinitionTypeProvider implements CredentialsDefinitionTypeProvider {
  private final ResourceLoader resourceLoader;
  private final ObjectProvider<SpinnakerPluginManager> pluginManagerProvider;
  private final List<String> scanPackages;

  private final Counter credentialsDefinitionClassLoadFailures =
      Metrics.counter("credentials.types.classLoadError");

  public DefaultCredentialsDefinitionTypeProvider(
      ResourceLoader resourceLoader,
      ObjectProvider<SpinnakerPluginManager> pluginManagerProvider,
      CredentialsDefinitionTypeProperties properties) {
    this.resourceLoader = resourceLoader;
    this.pluginManagerProvider = pluginManagerProvider;
    scanPackages = new ArrayList<>();
    // most classes in spinnaker are packaged under com.netflix.spinnaker;
    // however, kayenta is packaged under com.netflix.kayenta
    scanPackages.add("com.netflix");
    scanPackages.addAll(properties.getAdditionalScanPackages());
  }

  @Override
  public Map<String, Class<? extends CredentialsDefinition>> getCredentialsTypes() {
    var allScannables = new HashMap<String, ResourceLoader>();

    // Load all type providers from main app classes and configuration
    scanPackages.forEach(sp -> allScannables.put(sp, resourceLoader));

    // Load all defined type providers from plugins
    SpinnakerPluginManager pluginManager = pluginManagerProvider.getIfAvailable();
    if (pluginManager != null) {
      pluginManager
          .getStartedPlugins()
          .forEach(
              p -> {
                var pluginLoader = p.getPluginClassLoader();
                for (var pkg : p.getPluginClassLoader().getDefinedPackages()) {
                  allScannables.put(pkg.getName(), new DefaultResourceLoader(pluginLoader));
                }
              });
    }

    return allScannables.entrySet().stream()
        .flatMap(
            e -> {
              var provider = new ClassPathScanningCandidateComponentProvider(false);
              provider.setResourceLoader(e.getValue());
              provider.addIncludeFilter(new AssignableTypeFilter(CredentialsDefinition.class));
              return provider.findCandidateComponents(e.getKey()).stream()
                  .map(
                      beanDefinition ->
                          tryLoadAccountDefinitionClassName(
                              beanDefinition, e.getValue().getClassLoader()));
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
  }

  @Nullable
  private Map.Entry<String, Class<? extends CredentialsDefinition>>
      tryLoadAccountDefinitionClassName(
          BeanDefinition beanDefinition, @Nullable ClassLoader classLoader) {
    var className = beanDefinition.getBeanClassName();
    if (className == null) {
      return null;
    }

    try {
      Class<? extends CredentialsDefinition> subtype =
          ClassUtils.forName(className, classLoader).asSubclass(CredentialsDefinition.class);
      String typeName = CredentialsTypes.getCredentialsTypeName(subtype);
      if (typeName != null) {
        log.info("Discovered credentials definition type '{}' from class '{}'", typeName, subtype);
        return Map.entry(typeName, subtype);
      } else {
        log.info(
            "Skipping CredentialsDefinition class '{}' as it does not define a @CredentialsType annotation",
            subtype);
      }
    } catch (ClassNotFoundException e) {
      log.warn(
          "Unable to load CredentialsDefinition class '{}'. Credentials with this type will not be loaded.",
          className,
          e);
      credentialsDefinitionClassLoadFailures.increment();
    }
    return null;
  }
}
