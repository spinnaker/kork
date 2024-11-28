/*
 * Copyright 2024 Apple, Inc.
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

package com.netflix.spinnaker.kork.dynamicconfig;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import dev.openfeature.sdk.Features;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/** Handles creation of an OpenFeature-based {@link DynamicConfigService} bean. */
@Log4j2
@NonnullByDefault
@RequiredArgsConstructor
public class OpenFeatureImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
  private final Environment environment;
  private final BeanFactory beanFactory;

  @Override
  public void registerBeanDefinitions(
      AnnotationMetadata importingClassMetadata,
      BeanDefinitionRegistry registry,
      BeanNameGenerator importBeanNameGenerator) {
    var enableOpenFeatureFlags =
        environment.getProperty("flags.providers.openfeature.enabled", Boolean.class, true);
    if (!enableOpenFeatureFlags) {
      // no need to configure anything; let default SpringDynamicConfigService bean be created
      // elsewhere
      return;
    }
    var features = beanFactory.getBeanProvider(Features.class).getIfUnique();
    if (features == null) {
      return;
    }

    var dynamicConfigServiceFallbackBean = new GenericBeanDefinition();
    var enableSpringFlags =
        environment.getProperty("flags.providers.spring.enabled", Boolean.class, true);
    // not trying to auto-inject a dependent bean anywhere
    dynamicConfigServiceFallbackBean.setAutowireCandidate(false);
    if (enableSpringFlags) {
      dynamicConfigServiceFallbackBean.setBeanClass(SpringDynamicConfigService.class);
    } else {
      dynamicConfigServiceFallbackBean.setInstanceSupplier(() -> DynamicConfigService.NOOP);
    }

    var fallbackBeanName =
        importBeanNameGenerator.generateBeanName(dynamicConfigServiceFallbackBean, registry);
    registry.registerBeanDefinition(fallbackBeanName, dynamicConfigServiceFallbackBean);
    log.debug("Registered bean '{}': {}", fallbackBeanName, dynamicConfigServiceFallbackBean);

    var dynamicConfigServiceBean = new GenericBeanDefinition();
    dynamicConfigServiceBean.setPrimary(true);
    dynamicConfigServiceBean.setBeanClass(OpenFeatureDynamicConfigService.class);
    var args = dynamicConfigServiceBean.getConstructorArgumentValues();
    args.addGenericArgumentValue(features);
    args.addGenericArgumentValue(new RuntimeBeanReference(fallbackBeanName));
    var beanName = importBeanNameGenerator.generateBeanName(dynamicConfigServiceBean, registry);
    registry.registerBeanDefinition(beanName, dynamicConfigServiceBean);
    log.debug("Registered bean '{}': {}", beanName, dynamicConfigServiceBean);
  }
}
