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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.kork.annotations.Alpha;

/** Describes where these credentials come from. */
@Alpha
public enum CredentialsSource {
  /**
   * Describes credentials stored in {@linkplain CredentialsDefinitionRepository account storage}.
   */
  STORAGE,
  /** Describes credentials stored in configuration files. */
  CONFIG,
  /** Describes credentials provided by a plugin. */
  PLUGIN,
  /**
   * Describes credentials provided from an external source such as a custom resource definition.
   */
  EXTERNAL,
}
