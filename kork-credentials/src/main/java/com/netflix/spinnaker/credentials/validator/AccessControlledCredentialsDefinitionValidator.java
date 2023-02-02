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
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * Validates that an authenticated user has authorization to write with credentials. This helps to
 * prevent users from inadvertently locking themselves out from their own credentials.
 */
@Component
@NonnullByDefault
@RequiredArgsConstructor
public class AccessControlledCredentialsDefinitionValidator
    implements CredentialsDefinitionValidator {
  private final PermissionEvaluator permissionEvaluator;

  @Override
  public void validate(
      CredentialsDefinition definition, Errors errors, Authentication authentication) {
    if (definition instanceof AccessControlled
        && !permissionEvaluator.hasPermission(authentication, definition, Authorization.WRITE)) {
      String field = errors.getFieldType("permissions") != null ? "permissions" : "roles";
      errors.rejectValue(
          field,
          CredentialsDefinitionErrorCode.INVALID_PERMISSIONS.getErrorCode(),
          String.format(
              "Cannot save credentials definition without granting write permissions for current user (name: %s)",
              definition.getName()));
    }
  }
}
