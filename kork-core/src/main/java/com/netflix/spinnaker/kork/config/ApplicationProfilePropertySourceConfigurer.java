/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.kork.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.EnumerableCompositePropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static org.springframework.boot.context.config.ConfigFileApplicationListener.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME;

/**
 * Offers an (optional), standardized way of breaking up a Spinnaker service
 * application config file. Configs can be internal (JAR) or external
 * (filesystem), but must be YAML.
 *
 * Property sources are searched for by matching the format
 * "{appName}_{profile}". Example:
 *
 * For the "clouddriver" application and two active profiles, "test,us-west-2",
 * the following resources will be searched for:
 *
 * 1. FileSystemResource("clouddriver_us-west-2.yml")
 * 2. ClassPathResource("clouddriver_us-west-2.yml")
 * 3. FileSystemResource("clouddriver_test.yml")
 * 4. ClassPathResource("clouddriver_test.yml")
 *
 * These resources, if they exist, will be added just before the standard
 * Spring Boot "{appName}.yml" resource, so these files will have a higher
 * precedence than anything in that file.
 *
 * This configurer is shipped default disabled, but can be enabled by passing
 * the "kork.applicationProfileConfig.enabled=true" property.
 */
public class ApplicationProfilePropertySourceConfigurer implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

  private final static String PROPERTY_NAME = "kork.applicationProfileConfig.enabled";
  private final static Logger log = LoggerFactory.getLogger(ApplicationProfilePropertySourceConfigurer.class);
  private final static YamlPropertySourceLoader propertySourceLoader = new YamlPropertySourceLoader();

  @Override
  public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
    ConfigurableEnvironment environment = event.getEnvironment();
    if (isEnabled(environment)) {
      log.debug("Searching for application profile property sources");
      onInterestedEvent(environment);
    }
  }

  void onInterestedEvent(ConfigurableEnvironment environment) {
    String appName = environment.getProperty("spring.application.name");

    EnumerableCompositePropertySource ps = new EnumerableCompositePropertySource("applicationProfileConfigProperties");

    // Follows Spring Boot's load order: For each active profile, first load external, then internal
    // ... but Spring reads in the reverse way.
    List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
    Collections.reverse(activeProfiles);

    activeProfiles.stream()
      .map(it -> {
        String resourceName = format("%s_%s.yml", appName, it);
        log.trace("Searching for {}", resourceName);

        return Arrays.asList(
          new ProfileConfigResource(it, new ClassPathResource(resourceName)),
          new ProfileConfigResource(it, new FileSystemResource(resourceName))
        );
      })
      .flatMap(List::stream)
      .filter(it -> it.resource.exists())
      .forEach(profileConfig -> load(ps, profileConfig));

    environment.getPropertySources().addBefore(APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME, ps);

  }

  private void load(EnumerableCompositePropertySource compositePropertySource, ProfileConfigResource profileConfig) {
    final Resource resource = profileConfig.resource;
    try {
      PropertySource<?> ps = propertySourceLoader.load(resource.getFilename(), resource, profileConfig.profile);
      if (ps != null) {
        compositePropertySource.add(ps);
        log.info("Loaded PropertySource: " + resource.getFilename() + ":" + profileConfig.profile);
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not load profile config", e);
    }
  }

  private boolean isEnabled(Environment environment) {
    return Boolean.valueOf(environment.getProperty(PROPERTY_NAME, "false"));
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE - 100;
  }

  private static class ProfileConfigResource {
    final String profile;
    final Resource resource;

    public ProfileConfigResource(String profile, Resource resource) {
      this.profile = profile;
      this.resource = resource;
    }
  }
}

