/*
 * Copyright 2022 Netflix, Inc.
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

package com.netflix.spinnaker.kork.secrets.engines;

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.secrets.user.UserSecretSerdeFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretsManagerConfiguration {

  /**
   * Overriding this bean allows for further customizing which AWSSecretsManager client instance to
   * use to fetch a given secret by name and region. This can be particularly useful when using
   * multiple AWS regions or partitions.
   */
  @Bean
  @ConditionalOnMissingBean
  SecretsManagerClientProvider defaultSecretsManagerClientProvider() {
    return (region, name) -> AWSSecretsManagerClientBuilder.standard().withRegion(region).build();
  }

  @Bean
  SecretsManagerSecretEngine secretsManagerSecretEngine(
      ObjectMapper mapper,
      UserSecretSerdeFactory userSecretSerdeFactory,
      SecretsManagerClientProvider clientProvider) {
    return new SecretsManagerSecretEngine(mapper, userSecretSerdeFactory, clientProvider);
  }
}
