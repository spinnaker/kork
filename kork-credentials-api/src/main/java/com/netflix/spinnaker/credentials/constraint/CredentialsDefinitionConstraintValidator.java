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

package com.netflix.spinnaker.credentials.constraint;

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

/**
 * Constraint validator implementation for the {@link ValidatedCredentials} annotation. Validation
 * of credentials includes checking that its name matches the configured {@link
 * CredentialsDefinitionNamePatternProvider} bean-provided regular expression using the credentials
 * type name extracted by the configured {@link CredentialsDefinitionTypeExtractor} bean along with
 * any other {@link CredentialsDefinitionValidator} beans found at runtime.
 */
@RequiredArgsConstructor
public class CredentialsDefinitionConstraintValidator
    implements ConstraintValidator<ValidatedCredentials, CredentialsDefinition> {
  private final CredentialsDefinitionTypeExtractor typeExtractor;
  private final CredentialsDefinitionNamePatternProvider namePatternProvider;
  private final ObjectProvider<CredentialsDefinitionValidator> validators;

  @Override
  public boolean isValid(CredentialsDefinition value, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    String name = value.getName();
    if (!StringUtils.hasText(name)) {
      context
          .buildConstraintViolationWithTemplate("missing account name")
          .addPropertyNode("name")
          .addConstraintViolation();
      return false;
    }
    String type = typeExtractor.getCredentialsType(value);
    if (type == null) {
      context.buildConstraintViolationWithTemplate("missing account type").addConstraintViolation();
      return false;
    }
    Pattern pattern = namePatternProvider.getPatternForType(type);
    boolean valid = pattern.matcher(name).matches();
    if (!valid) {
      context
          .buildConstraintViolationWithTemplate("account name does not match pattern " + pattern)
          .addPropertyNode("name")
          .addConstraintViolation();
    }
    for (CredentialsDefinitionValidator validator : validators) {
      valid &= validator.isValid(value, context);
    }
    return valid;
  }
}
