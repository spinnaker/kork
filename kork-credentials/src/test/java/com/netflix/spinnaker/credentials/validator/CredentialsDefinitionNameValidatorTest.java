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

import static org.assertj.core.api.Assertions.assertThat;

import io.spinnaker.test.security.OverlyFlexibleAccount;
import io.spinnaker.test.security.ValidatedAccount;
import io.spinnaker.test.security.ValueAccount;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    properties = {"credentials.validator.valid-account-name-patterns.validated = f.+bar"})
class CredentialsDefinitionNameValidatorTest {
  @MockBean PermissionEvaluator permissionEvaluator;
  @Autowired Validator validator;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("test", "test"));
  }

  @Test
  void passesDefaultValidation() {
    var account = ValueAccount.builder().name("Foo-Bar_Account01").value("hello").build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isEmpty();
  }

  @Test
  void passesExplicitValidation() {
    var account = new ValidatedAccount();
    account.setName("foobar");
    account.setLabel("2000");

    Set<ConstraintViolation<ValidatedAccount>> violations = validator.validate(account);

    assertThat(violations).isEmpty();
  }

  @Test
  void failsDefaultValidation() {
    var account = ValueAccount.builder().name("---illegal-name---").value("hello").build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations)
        .hasSize(1)
        .first()
        .extracting(ConstraintViolation::getMessage)
        .asString()
        .contains("account name does not match pattern");
  }

  @Test
  void failsExplicitValidation() {
    var account = new ValidatedAccount();
    account.setName("bar_foo");
    account.setLabel("2000");

    Set<ConstraintViolation<ValidatedAccount>> violations = validator.validate(account);

    assertThat(violations)
        .hasSize(1)
        .first()
        .extracting(ConstraintViolation::getMessage)
        .asString()
        .contains("account name does not match pattern f.+bar");
  }

  @Test
  void failsWithNameTooShort() {
    var account = ValueAccount.builder().name("no").value("yes").build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations)
        .hasSize(1)
        .first()
        .extracting(ConstraintViolation::getMessage)
        .asString()
        .contains("account name does not match pattern");
  }

  @Test
  void failsWithNullName() {
    var account = new OverlyFlexibleAccount();

    Set<ConstraintViolation<OverlyFlexibleAccount>> violations = validator.validate(account);

    assertThat(violations).hasSize(1);
  }

  @Test
  void failsWithEmptyName() {
    var account = new OverlyFlexibleAccount();
    account.setName("");

    Set<ConstraintViolation<OverlyFlexibleAccount>> violations = validator.validate(account);

    assertThat(violations).hasSize(1);
  }

  @Test
  void failsWithBlankName() {
    var account = new OverlyFlexibleAccount();
    account.setName(" \t\r\n");

    Set<ConstraintViolation<OverlyFlexibleAccount>> violations = validator.validate(account);

    assertThat(violations).hasSize(1);
  }

  @Configuration
  @EnableAutoConfiguration
  static class Config {}
}
