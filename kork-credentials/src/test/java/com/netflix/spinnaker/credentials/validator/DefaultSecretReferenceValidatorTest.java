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
 *
 */

package com.netflix.spinnaker.credentials.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretErrorCode;
import com.netflix.spinnaker.kork.secrets.SecretReferenceValidator;
import com.netflix.spinnaker.kork.secrets.user.StringUserSecretData;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadata;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(SecurityTestConfig.class)
class DefaultSecretReferenceValidatorTest {
  @MockBean SecretEngine engine;
  @Autowired SecretReferenceValidator validator;

  String secretUri = "secret://mock?k=v";
  UserSecretReference secretReference = UserSecretReference.parse(secretUri);
  UserSecret userSecret =
      UserSecret.builder()
          .data(new StringUserSecretData("hunter2"))
          .metadata(
              UserSecretMetadata.builder().type("string").roles(List.of("secret-role")).build())
          .build();

  @BeforeEach
  void setUp() {
    given(engine.identifier()).willReturn("mock");
  }

  @Test
  @WithMockUser
  void unauthorizedUserIsDeniedAccessToUserSecret() {
    given(engine.decrypt(secretReference)).willReturn(userSecret);

    assertThat(validator.validate(secretUri))
        .isPresent()
        .hasValue(SecretErrorCode.DENIED_ACCESS_TO_USER_SECRET);
  }

  @Test
  @WithMockUser(roles = "secret-role")
  void authorizedUserCanAccessUserSecret() {
    given(engine.decrypt(secretReference)).willReturn(userSecret);

    assertThat(validator.validate(secretUri)).isEmpty();
  }
}
