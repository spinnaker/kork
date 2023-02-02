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

import com.netflix.spinnaker.credentials.secrets.SecretCredentialsConfiguration;
import com.netflix.spinnaker.credentials.service.CredentialsServiceConfiguration;
import com.netflix.spinnaker.credentials.types.CredentialsDefinitionTypesConfiguration;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionValidatorConfiguration;
import com.netflix.spinnaker.kork.secrets.SecretConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration classes for using the credentials storage and retrieval APIs. By adding a
 * dependency to {@code kork-credentials} and using {@link
 * org.springframework.boot.autoconfigure.EnableAutoConfiguration}, these configurations will be
 * included automatically.
 */
@Configuration
@Import({
  SecretConfiguration.class,
  SecretCredentialsConfiguration.class,
  CredentialsDefinitionTypesConfiguration.class,
  CredentialsDefinitionValidatorConfiguration.class,
  CredentialsServiceConfiguration.class,
})
public class CredentialsAutoConfiguration {}
