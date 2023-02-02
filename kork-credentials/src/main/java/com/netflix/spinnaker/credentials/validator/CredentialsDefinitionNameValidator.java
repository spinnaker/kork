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

import com.netflix.spinnaker.credentials.CredentialsTypes;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * Validates that credentials definition names follow a configurable pattern. This pattern may be
 * configured per-type or globally.
 *
 * @see CredentialsDefinitionValidatorProperties
 */
@Component
@NonnullByDefault
@RequiredArgsConstructor
public class CredentialsDefinitionNameValidator implements CredentialsDefinitionValidator {
  private final CredentialsDefinitionValidatorProperties properties;

  @Override
  public void validate(
      CredentialsDefinition definition, Errors errors, Authentication authentication) {
    String typeName = CredentialsTypes.getCredentialsTypeName(definition.getClass());
    Pattern pattern = properties.getValidAccountNamePattern(typeName);
    String name = definition.getName();
    if (!pattern.matcher(name).matches()) {
      errors.rejectValue(
          "name",
          CredentialsDefinitionErrorCode.INVALID_NAME.getErrorCode(),
          String.format(
              "Provided account name '%s' does not match regular expression %s", name, pattern));
    }
  }
}
