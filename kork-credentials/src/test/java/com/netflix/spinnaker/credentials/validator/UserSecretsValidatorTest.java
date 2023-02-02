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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionSecretManager;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.security.SpinnakerAuthorities;
import io.spinnaker.test.security.ValueAccount;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;

class UserSecretsValidatorTest {

  CredentialsDefinitionSecretManager secretManager = mock(CredentialsDefinitionSecretManager.class);
  UserSecretsValidator validator = new UserSecretsValidator(secretManager);
  Authentication authentication = new TestingAuthenticationToken("bugs", "bunny");
  Authentication admin =
      new TestingAuthenticationToken(
          "admin", "admin", List.of(SpinnakerAuthorities.ADMIN_AUTHORITY));

  @Test
  void canUseAuthorizedUserSecret() {
    UserSecret secret = mock(UserSecret.class);
    String secretUri = "secret://mock?fake=yes";
    UserSecretReference ref = UserSecretReference.parse(secretUri);
    given(secretManager.getUserSecret(eq(ref))).willReturn(secret);
    given(secretManager.canReadUserSecret(eq(authentication), eq(secret))).willReturn(true);

    ValueAccount account = ValueAccount.builder().name("elmer").value(secretUri).build();
    var errors = new BeanPropertyBindingResult(account, account.getName());
    validator.validate(account, errors, authentication);

    assertFalse(errors.hasErrors());
  }

  @Test
  void cannotUseUnauthorizedUserSecret() {
    UserSecret secret = mock(UserSecret.class);
    String secretUri = "secret://mock?fake=yes";
    UserSecretReference ref = UserSecretReference.parse(secretUri);
    given(secretManager.getUserSecret(eq(ref))).willReturn(secret);
    given(secretManager.canReadUserSecret(any(), eq(secret))).willReturn(false);

    ValueAccount account = ValueAccount.builder().name("daffy").value(secretUri).build();
    var errors = new BeanPropertyBindingResult(account, account.getName());
    validator.validate(account, errors, authentication);

    assertTrue(errors.hasErrors());
  }

  @Test
  void cannotUseEncryptedSecretWithoutAdmin() {
    String secretUri = "encrypted:mock!fake:yes";
    EncryptedSecret ref = EncryptedSecret.parse(secretUri);
    assertNotNull(ref);
    ValueAccount account = ValueAccount.builder().name("donald").value(secretUri).build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, authentication);

    assertTrue(errors.hasErrors());
  }

  @Test
  void canUseEncryptedSecretWhenAdmin() {
    String secretUri = "encrypted:mock!fake:yes";
    EncryptedSecret ref = EncryptedSecret.parse(secretUri);
    assertNotNull(ref);
    ValueAccount account = ValueAccount.builder().name("donald").value(secretUri).build();
    var errors = new BeanPropertyBindingResult(account, account.getName());

    validator.validate(account, errors, admin);

    assertFalse(errors.hasErrors());
  }

  @Test
  void cannotUseEncryptedSecretWithDecryptError() {
    String secretUri = "encrypted:mock!fake:yes";
    EncryptedSecret ref = EncryptedSecret.parse(secretUri);
    assertNotNull(ref);
    ValueAccount account = ValueAccount.builder().name("donald").value(secretUri).build();
    var errors = new BeanPropertyBindingResult(account, account.getName());
    given(secretManager.getExternalSecretString(eq(ref)))
        .willThrow(SecretDecryptionException.class);

    validator.validate(account, errors, admin);

    assertTrue(errors.hasErrors());
  }
}
