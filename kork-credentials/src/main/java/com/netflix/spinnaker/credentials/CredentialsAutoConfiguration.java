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

package com.netflix.spinnaker.credentials;

import com.netflix.spinnaker.credentials.poller.PollerConfiguration;
import com.netflix.spinnaker.credentials.poller.PollerConfigurationProperties;
import com.netflix.spinnaker.credentials.secrets.SecretCredentialsConfiguration;
import com.netflix.spinnaker.credentials.service.CredentialsServiceConfiguration;
import com.netflix.spinnaker.credentials.types.CredentialsDefinitionTypesConfiguration;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionValidatorConfiguration;
import com.netflix.spinnaker.kork.secrets.SecretConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfigureAfter({
  JacksonAutoConfiguration.class,
  ValidationAutoConfiguration.class,
  SecurityAutoConfiguration.class
})
@Import({
  CredentialsDefinitionTypesConfiguration.class,
  SecretConfiguration.class,
  SecretCredentialsConfiguration.class,
  CredentialsDefinitionValidatorConfiguration.class,
  CredentialsServiceConfiguration.class,
  CredentialsManager.class,
  PollerConfiguration.class,
})
@EnableConfigurationProperties(PollerConfigurationProperties.class)
public class CredentialsAutoConfiguration {
  @Bean
  public static CredentialsTypePropertiesBeanPostProcessor
      credentialsTypePropertiesBeanPostProcessor() {
    return new CredentialsTypePropertiesBeanPostProcessor();
  }
}
