/*
 * Copyright 2020 Armory
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

package com.netflix.spinnaker.credentials.definition;

import com.netflix.spinnaker.credentials.Credentials;
import com.netflix.spinnaker.credentials.constraint.ValidatedCredentials;

/**
 * Contains properties that define {@link Credentials}. {@link CredentialsDefinition} can be POJOs
 * deserialized from configuration or an external system. These are optional but useful to use
 * built-in {@link CredentialsParser}. Implementations that wish to support account storage must
 * also be annotated with {@link CredentialsType} corresponding to the type discriminator to use for
 * serialization and deserialization.
 *
 * <p>equals is checked to detect change in definitions
 */
@ValidatedCredentials
public interface CredentialsDefinition {
  String getName();
}
