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
 *
 */

package com.netflix.spinnaker.credentials;

import com.netflix.spinnaker.credentials.definition.AbstractCredentialsLoader;
import com.netflix.spinnaker.credentials.definition.BasicCredentialsLoader;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.credentials.definition.CredentialsParser;
import com.netflix.spinnaker.credentials.service.CompositeCredentialsDefinitionSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

/**
 * Helper class for {@link CredentialsTypePropertiesBeanPostProcessor}. Handles registration and
 * loading of default credentials beans for a {@link CredentialsTypeProperties} bean.
 *
 * @param <C> type of Credentials this manages
 * @param <D> type of CredentialsDefinitions this uses
 * @see CredentialsTypePropertiesBeanPostProcessor
 */
@Log4j2
@SuppressWarnings("unchecked")
class CredentialsTypePropertiesBeanLoader<C extends Credentials, D extends CredentialsDefinition> {
  private final CredentialsTypeProperties<C, D> properties;
  private final Environment environment;
  private final ConfigurableListableBeanFactory beanFactory;
  private final BeanDefinitionRegistry registry;

  // CredentialsRepository<C>
  private final ResolvableType credentialsRepositoryType;

  // CredentialsDefinitionSource<D>
  private final ResolvableType credentialsDefinitionSourceType;

  // CredentialsParser<D, C>
  private final ResolvableType credentialsParserType;

  // AbstractCredentialsLoader<C>
  private final ResolvableType credentialsLoaderType;

  CredentialsTypePropertiesBeanLoader(
      ApplicationContext context, CredentialsTypeProperties<C, D> properties) {
    this.properties = properties;

    environment = context.getEnvironment();
    beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
    registry = (BeanDefinitionRegistry) beanFactory;

    // cache some resolvable types for bean lookup and registration
    credentialsRepositoryType =
        ResolvableType.forClassWithGenerics(
            CredentialsRepository.class, properties.getCredentialsClass());
    credentialsDefinitionSourceType =
        ResolvableType.forClassWithGenerics(
            CredentialsDefinitionSource.class, properties.getCredentialsDefinitionClass());
    credentialsParserType =
        ResolvableType.forClassWithGenerics(
            CredentialsParser.class,
            properties.getCredentialsDefinitionClass(),
            properties.getCredentialsClass());
    credentialsLoaderType =
        ResolvableType.forClassWithGenerics(
            AbstractCredentialsLoader.class, properties.getCredentialsClass());
  }

  /** Loads and registers credentials beans for these properties. */
  void initializeCredentialsLoader() {
    String beanName =
        findExistingBeanName(credentialsLoaderType)
            .orElseGet(
                () ->
                    registerCredentialsLoader(
                        getReferenceToCredentialsDefinitionSource(), getCredentialsRepository()));
    // initialize the bean if it hasn't been already
    beanFactory.getBean(beanName);
  }

  /**
   * Gets a bean reference to the CredentialsDefinitionSource for these properties. This will use an
   * existing bean if available or will handle registration of a default bean otherwise. Bean
   * initialization is deferred until it is needed by a CredentialsLoader.
   */
  private BeanReference getReferenceToCredentialsDefinitionSource() {
    return findExistingBeanName(credentialsDefinitionSourceType)
        .map(RuntimeBeanReference::new)
        .orElseGet(this::registerCredentialsDefinitionSource);
  }

  /**
   * Registers a CredentialsDefinitionSource bean for these properties. If credentials storage is
   * both enabled and opted in for this credential definition type, then this decorates the default
   * credential definition source with a storage-aware version.
   */
  private RuntimeBeanReference registerCredentialsDefinitionSource() {
    var bd = new RootBeanDefinition();
    getCredentialsStorageRepositoryReferenceIfEnabled()
        .ifPresentOrElse(
            repositoryBeanReference -> {
              log.debug("Credentials storage enabled for {}", properties);
              bd.setBeanClass(CompositeCredentialsDefinitionSource.class);
              Class<D> definitionClass = properties.getCredentialsDefinitionClass();
              bd.setTargetType(
                  ResolvableType.forClassWithGenerics(
                      CompositeCredentialsDefinitionSource.class, definitionClass));
              bd.getConstructorArgumentValues().addGenericArgumentValue(repositoryBeanReference);
              bd.getConstructorArgumentValues().addGenericArgumentValue(definitionClass);
              // TODO(jvz): consider supporting multiple CredentialsDefinitionSource beans here
              bd.getConstructorArgumentValues()
                  .addGenericArgumentValue(List.of(properties.getDefaultCredentialsSource()));
            },
            () -> {
              log.debug("Credentials storage disabled for {}", properties);
              bd.setBeanClass(CredentialsDefinitionSource.class);
              bd.setTargetType(credentialsDefinitionSourceType);
              bd.setInstanceSupplier(properties::getDefaultCredentialsSource);
            });
    return new RuntimeBeanReference(
        BeanDefinitionReaderUtils.registerWithGeneratedName(bd, registry));
  }

  /**
   * Gets the CredentialsRepository for these properties. This will try to use an existing bean if
   * available or will otherwise register and initialize a default one.
   */
  private CredentialsRepository<C> getCredentialsRepository() {
    return findExistingBeanName(credentialsRepositoryType)
        .map(beanName -> beanFactory.getBean(beanName, CredentialsRepository.class))
        .orElseGet(this::registerCredentialsRepository);
  }

  /**
   * Registers a CredentialsRepository bean for these properties. This will use an existing
   * CredentialsLifecycleHandler bean for this credential type if available.
   */
  private CredentialsRepository<C> registerCredentialsRepository() {
    var bd = new RootBeanDefinition(MapBackedCredentialsRepository.class);
    Class<C> credentialsClass = properties.getCredentialsClass();
    String type = properties.getType();
    bd.setTargetType(
        ResolvableType.forClassWithGenerics(
            MapBackedCredentialsRepository.class, credentialsClass));
    bd.getConstructorArgumentValues().addGenericArgumentValue(type);
    ResolvableType lifecycleHandlerType =
        ResolvableType.forClassWithGenerics(CredentialsLifecycleHandler.class, credentialsClass);
    findExistingBeanName(lifecycleHandlerType)
        .ifPresentOrElse(
            beanName ->
                bd.getConstructorArgumentValues()
                    .addGenericArgumentValue(new RuntimeBeanReference(beanName)),
            () ->
                bd.getConstructorArgumentValues()
                    .addGenericArgumentValue(new NoopCredentialsLifecycleHandler<C>()));
    String beanName = "credentialsRepository." + type;
    registry.registerBeanDefinition(beanName, bd);
    log.debug("Registered bean named '{}' with type {}", beanName, credentialsRepositoryType);
    return beanFactory.getBean(beanName, CredentialsRepository.class);
  }

  /** Registers a BasicCredentialsLoader bean using the beans configured for these properties. */
  private String registerCredentialsLoader(
      BeanReference credentialsDefinitionSourceReference, CredentialsRepository<C> repository) {
    var bd = new RootBeanDefinition(BasicCredentialsLoader.class);
    bd.setTargetType(
        ResolvableType.forClassWithGenerics(
            BasicCredentialsLoader.class,
            properties.getCredentialsDefinitionClass(),
            properties.getCredentialsClass()));
    bd.getConstructorArgumentValues().addGenericArgumentValue(credentialsDefinitionSourceReference);
    findExistingBeanName(credentialsParserType)
        .ifPresentOrElse(
            beanName ->
                bd.getConstructorArgumentValues()
                    .addGenericArgumentValue(new RuntimeBeanReference(beanName)),
            () ->
                bd.getConstructorArgumentValues()
                    .addGenericArgumentValue(
                        Objects.requireNonNull(
                            properties.getCredentialsParser(),
                            () -> "No " + credentialsParserType + " found")));
    bd.getConstructorArgumentValues().addGenericArgumentValue(repository);
    bd.getConstructorArgumentValues().addGenericArgumentValue(properties.isParallel());
    String beanName = "credentialsLoader." + properties.getType();
    registry.registerBeanDefinition(beanName, bd);
    log.debug("Registered bean named '{}' with type {}", beanName, bd.getResolvableType());
    return beanName;
  }

  /**
   * Finds an existing bean name matching the provided type. Returns empty when there are no
   * matching beans or the bean name if there is a unique bean; otherwise, throws a {@link
   * NoUniqueBeanDefinitionException} if more than one bean matches the given type.
   */
  private Optional<String> findExistingBeanName(ResolvableType type) {
    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type);
    if (beanNames.length > 1) {
      throw new NoUniqueBeanDefinitionException(type, beanNames);
    }
    return beanNames.length == 1 ? Optional.of(beanNames[0]) : Optional.empty();
  }

  /**
   * Gets a bean reference for the CredentialsDefinitionRepository bean if credentials storage is
   * enabled for this credential type and credentials storage is enabled in general.
   */
  private Optional<BeanReference> getCredentialsStorageRepositoryReferenceIfEnabled() {
    if (isCredentialsStorageEnabled()) {
      String[] beanNames =
          BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
              beanFactory, CredentialsDefinitionRepository.class);
      if (beanNames.length > 1) {
        throw new NoUniqueBeanDefinitionException(CredentialsDefinitionRepository.class, beanNames);
      }
      return beanNames.length == 1
          ? Optional.of(new RuntimeBeanReference(beanNames[0]))
          : Optional.empty();
    }
    return Optional.empty();
  }

  private boolean isCredentialsStorageEnabled() {
    String typeName =
        CredentialsTypes.getCredentialsTypeName(properties.getCredentialsDefinitionClass());
    if (typeName == null) {
      return false;
    }
    String primaryProperty = "credentials.storage." + typeName + ".enabled";
    if (environment.containsProperty(primaryProperty)) {
      return environment.getRequiredProperty(primaryProperty, Boolean.class);
    }
    String legacyProperty = "account.storage." + typeName + ".enabled";
    return environment.getProperty(legacyProperty, Boolean.class, false);
  }
}
