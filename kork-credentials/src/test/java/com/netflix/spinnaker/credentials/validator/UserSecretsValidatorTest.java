/*
 * Copyright 2022 Apple Inc.
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretException;
import com.netflix.spinnaker.kork.secrets.user.OpaqueUserSecretData;
import com.netflix.spinnaker.kork.secrets.user.StringUserSecretData;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretData;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadata;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.security.SpinnakerAuthorities;
import io.spinnaker.test.security.OptionalFieldAccount;
import io.spinnaker.test.security.TestAccount;
import io.spinnaker.test.security.ValueAccount;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

@SpringJUnitConfig(SecurityTestConfig.class)
class UserSecretsValidatorTest {

  @MockBean SecretEngine mockSecretEngine;
  @Autowired Validator validator;

  UserSecret userSecret =
      UserSecret.builder()
          .data(new StringUserSecretData("hunter2"))
          .metadata(
              UserSecretMetadata.builder().type("string").roles(List.of("secret-role")).build())
          .build();
  String secretUri = "secret://mock?k=v";
  UserSecretReference ref = UserSecretReference.parse(secretUri);

  @BeforeEach
  void setUp() {
    given(mockSecretEngine.identifier()).willReturn("mock");
  }

  @Test
  @WithMockUser(roles = "secret-role")
  void canUseAuthorizedUserSecret() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    ValueAccount account = ValueAccount.builder().name("elmer").value(secretUri).build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isEmpty();
  }

  @Test
  @WithMockUser
  void cannotUseUnauthorizedUserSecret() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    ValueAccount account = ValueAccount.builder().name("daffy").value(secretUri).build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser
  void cannotUseUnauthorizedUserSecretInOptionalFields() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    OptionalFieldAccount account =
        OptionalFieldAccount.builder().name("opt").secret(Optional.of(secretUri)).build();

    var violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser
  void cannotUseUnauthorizedUserSecretInJsonAnyGetterMapValues() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    TestAccount account = new TestAccount();
    account.setData("name", "opt");
    account.setData("client-secret", secretUri);

    var violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser
  void cannotUseUnauthorizedUserSecretInJsonAnyGetterMapOptionalValues() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    TestAccount account = new TestAccount();
    account.setData("name", "opt");
    account.setData("client-secret", Optional.of(secretUri));

    var violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser
  void cannotUseEncryptedSecretWithoutAdmin() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    ValueAccount account =
        ValueAccount.builder().name("donald").value("encrypted:mock!fake:yes").build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser(authorities = SpinnakerAuthorities.ADMIN)
  void canUseEncryptedSecretWhenAdmin() {
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(userSecret);
    ValueAccount account =
        ValueAccount.builder().name("donald").value("encrypted:mock!fake:yes").build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isEmpty();
  }

  @Test
  @WithMockUser
  void canUseEncryptedSecretWhenSystemContextBindHandlerUsed() {
    Map<String, String> properties =
        Map.of("account.name", "donald", "account.value", "encrypted:mock!fake:yes");
    Binder binder = new Binder(new MapConfigurationPropertySource(properties));
    Bindable<ValueAccount> bindable = Bindable.of(ValueAccount.class);

    assertThrows(
        BindException.class, () -> binder.bindOrCreate("account", bindable, getBindHandler()));

    assertDoesNotThrow(
        () ->
            binder.bindOrCreate(
                "account", bindable, new SystemContextBindHandler(getBindHandler())));
  }

  private BindHandler getBindHandler() {
    return new ValidationBindHandler(new SpringValidatorAdapter(validator));
  }

  @Test
  @WithMockUser
  void cannotUseEncryptedSecretWithDecryptError() {
    String secretUri = "encrypted:mock!fake:yes";
    EncryptedSecret ref = EncryptedSecret.parse(secretUri);
    assertNotNull(ref);
    ValueAccount account = ValueAccount.builder().name("donald").value(secretUri).build();
    given(mockSecretEngine.decrypt(eq(ref))).willThrow(SecretDecryptionException.class);

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser
  void doesNotBreakWhenSecretExceptionIsThrown() {
    given(mockSecretEngine.decrypt(eq(ref))).willThrow(SecretException.class);

    ValueAccount account = ValueAccount.builder().name("error").value(secretUri).build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @WithMockUser
  void verifiesKeyExistsInSecret() {
    String secretUri = "secret://mock?k=foo";
    UserSecretReference ref = UserSecretReference.parse(secretUri);
    assertNotNull(ref);
    UserSecretMetadata metadata =
        UserSecretMetadata.builder().type("opaque").encoding("json").roles(List.of("user")).build();
    UserSecretData data = new OpaqueUserSecretData(Map.of());
    UserSecret secret = UserSecret.builder().metadata(metadata).data(data).build();
    given(mockSecretEngine.decrypt(eq(ref))).willReturn(secret);

    ValueAccount account = ValueAccount.builder().name("missing-key").value(secretUri).build();

    Set<ConstraintViolation<ValueAccount>> violations = validator.validate(account);
    assertThat(violations).isNotEmpty();
  }
}
