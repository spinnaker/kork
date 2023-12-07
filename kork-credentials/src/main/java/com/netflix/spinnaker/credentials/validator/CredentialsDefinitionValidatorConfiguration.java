/*
 * Copyright 2023 Apple Inc.
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

package com.netflix.spinnaker.credentials.validator;

import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionValidator;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Configures integration with {@link CredentialsDefinitionValidator}. */
@Configuration
@ComponentScan
@EnableConfigurationProperties(CredentialsDefinitionValidatorProperties.class)
public class CredentialsDefinitionValidatorConfiguration {
  @Bean
  public ConfigurationPropertiesBindHandlerAdvisor systemContextBindHandlerAdvisor() {
    return SystemContextBindHandler::new;
  }
}
