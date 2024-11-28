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

package com.netflix.spinnaker.kork.secrets.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.netflix.spinnaker.kork.secrets.SecretEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class UserSecretServiceTest {
  @MockBean SecretEngine mockSecretEngine;
  @Autowired UserSecretService userSecretService;

  UserSecret secret =
      UserSecret.builder()
          .data(new StringUserSecretData("super-secret"))
          .metadata(UserSecretMetadata.builder().type("string").build())
          .build();

  @BeforeEach
  void setUp() {
    given(mockSecretEngine.identifier()).willReturn("mock");
  }

  @Test
  void tracksUserSecret() {
    UserSecretReference ref = UserSecretReference.parse("secret://mock?k=v");
    given(mockSecretEngine.decrypt(ref)).willReturn(secret);

    String resourceId = "some-resource-id";
    assertFalse(userSecretService.isTrackingUserSecretsForResource(resourceId));
    String secretString = userSecretService.getUserSecretStringForResource(ref, resourceId);
    assertEquals("super-secret", secretString);
    assertTrue(userSecretService.isTrackingUserSecretsForResource(resourceId));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class TestConfig {}
}
