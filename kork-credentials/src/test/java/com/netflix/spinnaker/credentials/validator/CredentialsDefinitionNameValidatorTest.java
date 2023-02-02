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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spinnaker.test.security.ValueAccount;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;

class CredentialsDefinitionNameValidatorTest {
  Authentication authentication = new TestingAuthenticationToken("sherlock", "holmes");

  @Test
  void passesDefaultValidation() {
    var validator =
        new CredentialsDefinitionNameValidator(new CredentialsDefinitionValidatorProperties());
    var account = ValueAccount.builder().name("Foo-Bar_Account01").value("hello").build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, authentication);

    assertFalse(errors.hasErrors());
  }

  @Test
  void passesExplicitValidation() {
    var properties = new CredentialsDefinitionValidatorProperties();
    properties.setValidAccountNamePatterns(Map.of("value", Pattern.compile("foobar")));
    var validator = new CredentialsDefinitionNameValidator(properties);
    var account = ValueAccount.builder().name("foobar").value("2000").build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, authentication);

    assertFalse(errors.hasErrors());
  }

  @Test
  void failsDefaultValidation() {
    var validator =
        new CredentialsDefinitionNameValidator(new CredentialsDefinitionValidatorProperties());
    var account = ValueAccount.builder().name("---illegal-name---").value("hello").build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, authentication);

    assertTrue(errors.hasErrors());
  }

  @Test
  void failsExplicitValidation() {
    var properties = new CredentialsDefinitionValidatorProperties();
    properties.setValidAccountNamePatterns(Map.of("value", Pattern.compile("foobar")));
    var validator = new CredentialsDefinitionNameValidator(properties);
    var account = ValueAccount.builder().name("bar_foo").value("2000").build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, authentication);

    assertTrue(errors.hasErrors());
  }

  @Test
  void failsWithNameTooShort() {
    var validator =
        new CredentialsDefinitionNameValidator(new CredentialsDefinitionValidatorProperties());
    var account = ValueAccount.builder().name("no").value("yes").build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, authentication);

    assertTrue(errors.hasErrors());
  }
}
