/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.kork.secrets;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class SecretApplicationContextInitializer implements ApplicationContextInitializer {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    // Register servlet initializer so it is instantiated
    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setBeanClass(SecretServletContextInitializer.class);
    ((BeanDefinitionRegistry) applicationContext.getBeanFactory()).registerBeanDefinition("secretServletContextInitializer", bd);
    applicationContext.getBeanFactory().addBeanPostProcessor(new SecretBeanPostProcessor(applicationContext));
  }
}
