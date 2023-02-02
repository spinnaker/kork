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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Errors;

/**
 * Validates account definitions before storing them in a {@link CredentialsDefinitionRepository}.
 * All beans of this type are invoked on account definitions before being stored in the repository.
 */
@NonnullByDefault
public interface CredentialsDefinitionValidator {
  /**
   * Validates the provided credentials definition for the authenticated user and adds any
   * encountered errors.
   */
  void validate(CredentialsDefinition definition, Errors errors, Authentication authentication);
}
