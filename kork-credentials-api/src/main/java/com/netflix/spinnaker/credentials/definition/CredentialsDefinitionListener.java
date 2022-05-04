/*
 * Copyright 2022 Apple Inc.
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

package com.netflix.spinnaker.credentials.definition;

import com.netflix.spinnaker.kork.annotations.Beta;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;

/**
 * Listener for changes made to a type of credentials definitions.
 *
 * @param <T> the credential definition type to listen for changes
 */
@Beta
@NonnullByDefault
public interface CredentialsDefinitionListener<T extends CredentialsDefinition> {

  boolean supportsType(String type);

  /**
   * Invoked when credentials definitions are added or updated.
   *
   * @param definition the credential definition that was changed
   */
  void onDefinitionChanged(T definition);

  /**
   * Invoked when credentials definitions are removed.
   *
   * @param name the name of the credential definition that was removed
   */
  void onDefinitionRemoved(String name);
}
