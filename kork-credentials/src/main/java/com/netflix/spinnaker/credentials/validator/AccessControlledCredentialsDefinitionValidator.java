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

import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionValidator;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import com.netflix.spinnaker.security.SpinnakerAuthorities;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Validates that an authenticated user has authorization to write with credentials. This helps to
 * prevent users from inadvertently locking themselves out from their own credentials.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AccessControlledCredentialsDefinitionValidator
    implements CredentialsDefinitionValidator {
  private final PermissionEvaluator permissionEvaluator;

  @Override
  public boolean isValid(CredentialsDefinition value, ConstraintValidatorContext context) {
    if (!(value instanceof AccessControlled)) {
      // not our problem
      return true;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // when authentication is an admin user, they can't lock themselves out
    if (SpinnakerAuthorities.isAdmin(authentication)) {
      return true;
    }
    if (authentication == null) {
      context
          .buildConstraintViolationWithTemplate("no authenticated user found")
          .addConstraintViolation();
      return false;
    }
    if (!permissionEvaluator.hasPermission(authentication, value, Authorization.WRITE)) {
      BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
      String property = wrapper.isReadableProperty("permissions") ? "permissions" : "roles";
      log.debug(
          "No write permission granted to account {} in {} to {}",
          value.getName(),
          wrapper.getPropertyValue(property),
          authentication);
      context
          .buildConstraintViolationWithTemplate("no write permission granted to current user")
          .addPropertyNode(property)
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
