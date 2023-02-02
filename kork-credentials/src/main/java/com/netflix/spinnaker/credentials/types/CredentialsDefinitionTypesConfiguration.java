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

package com.netflix.spinnaker.credentials.types;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Provides configuration settings related to managing credential definitions at runtime.
 *
 * @see CredentialsDefinitionTypeProperties
 */
@Configuration
@EnableConfigurationProperties
@Log4j2
@ComponentScan
public class CredentialsDefinitionTypesConfiguration {
  @Bean
  @ConfigurationProperties("credentials.types")
  public CredentialsDefinitionTypeProperties credentialsDefinitionTypeProperties() {
    return new CredentialsDefinitionTypeProperties();
  }
}