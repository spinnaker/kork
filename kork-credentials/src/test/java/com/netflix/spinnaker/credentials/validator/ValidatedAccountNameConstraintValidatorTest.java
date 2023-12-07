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

import io.spinnaker.test.security.ValidatedAccount;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;

@SpringBootTest
class ValidatedAccountNameConstraintValidatorTest {
  @Autowired Validator validator;
  @MockBean PermissionEvaluator permissionEvaluator;

  @Test
  void nullValuesValidated() {
    var account = new ValidatedAccount();
    assertThat(validator.validate(account)).hasSize(2);
  }

  @Test
  void emptyValuesValidated() {
    var account = new ValidatedAccount();
    account.setName("");
    account.setLabel("");
    assertThat(validator.validate(account)).hasSize(2);
  }

  @Test
  void invalidNameValidation() {
    var account = new ValidatedAccount();
    account.setName("/etc/passwd");
    account.setLabel("/etc/shadow");
    assertThat(validator.validate(account)).hasSize(1);
  }

  @Configuration
  @EnableAutoConfiguration
  static class Config {}
}
