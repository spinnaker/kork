/*
 * Copyright 2023 Apple Inc.
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

package com.netflix.spinnaker.credentials.validator;

import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionValidator;
import java.net.URL;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

/**
 * A base class for {@link CredentialsDefinitionValidator} implementations. Provides common methods
 * used by the validators.
 */
@Log4j2
public abstract class AbstractCredentialsDefinitionValidator
    implements CredentialsDefinitionValidator {

  protected boolean emptyOptionalAndValue(final Optional<String> optValue) {
    return optValue.isEmpty() || (optValue.get() == null || optValue.get().isEmpty());
  }

  protected boolean isValidUrl(final String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) {
      return false;
    }
    try {
      new URL(endpoint).toURI();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  protected boolean logAndReturnValidationFailure(
      final ConstraintValidatorContext context, String fieldName, String message) {
    log.info("Account validation error for field '{}': {}", fieldName, message);
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(fieldName)
        .addConstraintViolation();
    return false;
  }
}
