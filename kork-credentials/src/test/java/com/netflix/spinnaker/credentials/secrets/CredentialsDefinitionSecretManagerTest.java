/*
 * Copyright 2022 Armory
 * Copyright 2022, 2023 Apple Inc.
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

package com.netflix.spinnaker.credentials.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.netflix.spinnaker.kork.secrets.user.*;
import com.netflix.spinnaker.security.Authorization;
import com.netflix.spinnaker.security.SpinnakerAuthorities;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CredentialsDefinitionSecretManagerTest {

  @Mock UserSecretManager userSecretManager;

  @Mock PermissionEvaluator permissionEvaluator;

  CredentialsDefinitionSecretManager secretManager;

  @BeforeEach
  void setUp() {
    secretManager = new CredentialsDefinitionSecretManager(userSecretManager, permissionEvaluator);
  }

  @Test
  void canAccessUserSecret() {
    var userSecret =
        UserSecret.builder()
            .data(new OpaqueUserSecretData(Map.of("foo", "bar")))
            .metadata(
                UserSecretMetadata.builder()
                    .type("opaque")
                    .encoding("json")
                    .roles(List.of("group", "group2"))
                    .build())
            .build();
    given(userSecretManager.getUserSecret(any())).willReturn(userSecret);
    var username = "user";
    var authentication = new TestingAuthenticationToken(username, "hunter2", "ROLE_group");
    var accountName = "account";
    given(
            permissionEvaluator.hasPermission(
                eq(authentication), eq(userSecret), eq(Authorization.READ)))
        .willReturn(true);
    given(
            permissionEvaluator.hasPermission(
                eq(authentication), eq(accountName), eq("account"), eq(Authorization.WRITE)))
        .willReturn(true);

    var ref = UserSecretReference.parse("secret://test?k=foo");
    assertThat(secretManager.getUserSecretString(ref, accountName)).isEqualTo("bar");
    assertThat(secretManager.canAccessAccountWithSecrets(authentication, accountName)).isTrue();
  }

  @Test
  void adminHasAccess() {
    var userSecret =
        UserSecret.builder()
            .data(new OpaqueUserSecretData(Map.of("foo", "bar")))
            .metadata(
                UserSecretMetadata.builder()
                    .type("opaque")
                    .encoding("cbor")
                    .roles(List.of())
                    .build())
            .build();
    given(userSecretManager.getUserSecret(any())).willReturn(userSecret);
    var authentication = new TestingAuthenticationToken("sphere", "pi", SpinnakerAuthorities.ADMIN);
    var ref = UserSecretReference.parse("secret://test?k=foo");
    var accountName = "cube";
    assertThat(secretManager.getUserSecretString(ref, accountName)).isEqualTo("bar");
    assertThat(secretManager.canAccessAccountWithSecrets(authentication, accountName)).isTrue();
  }

  @Test
  void cannotAccessUserSecret() {
    var userSecret =
        UserSecret.builder()
            .data(new OpaqueUserSecretData(Map.of("foo", "bar")))
            .metadata(
                UserSecretMetadata.builder()
                    .type("opaque")
                    .encoding("yaml")
                    .roles(List.of("group0", "group1"))
                    .build())
            .build();
    given(userSecretManager.getUserSecret(any())).willReturn(userSecret);
    given(permissionEvaluator.hasPermission(any(), any(), any())).willReturn(false);

    var accountName = "cube";
    var ref = UserSecretReference.parse("secret://test?k=foo");
    assertThat(secretManager.getUserSecretString(ref, accountName)).isEqualTo("bar");
    assertThat(
            secretManager.canAccessAccountWithSecrets(
                new TestingAuthenticationToken("sphere", "box"), accountName))
        .isFalse();
  }

  @Test
  void canAccessSecretButNotAccount() {
    var userSecret =
        UserSecret.builder()
            .data(new OpaqueUserSecretData(Map.of("foo", "bar")))
            .metadata(
                UserSecretMetadata.builder()
                    .type("opaque")
                    .encoding("json")
                    .roles(List.of("group0", "group1"))
                    .build())
            .build();
    given(userSecretManager.getUserSecret(any())).willReturn(userSecret);
    var authentication = new TestingAuthenticationToken("sphere", "circle");
    given(
            permissionEvaluator.hasPermission(
                eq(authentication), any(UserSecret.class), eq(Authorization.READ)))
        .willReturn(true);
    given(permissionEvaluator.hasPermission(eq(authentication), any(), eq("account"), any()))
        .willReturn(false);

    var accountName = "cube";
    var ref = UserSecretReference.parse("secret://test?k=foo");
    assertThat(secretManager.getUserSecretString(ref, accountName)).isEqualTo("bar");
    assertThat(secretManager.canAccessAccountWithSecrets(authentication, accountName)).isFalse();
  }
}
