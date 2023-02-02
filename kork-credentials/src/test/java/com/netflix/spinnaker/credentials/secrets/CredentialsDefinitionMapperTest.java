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

package com.netflix.spinnaker.credentials.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.credentials.CredentialsAutoConfiguration;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import io.spinnaker.test.security.TestAccount;
import io.spinnaker.test.security.ValueAccount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = "credentials.types.additional-scan-packages=io.spinnaker.test.security")
class CredentialsDefinitionMapperTest {

  @Autowired ObjectMapper objectMapper;
  @Autowired CredentialsDefinitionMapper mapper;
  @MockBean PermissionEvaluator fiatPermissionEvaluator;

  @Test
  void canConvertAdditionalAccountTypes() throws JsonProcessingException {
    var account = new TestAccount();
    account.setData("name", "foo");
    account.setData("password", "hunter2");
    Assertions.assertEquals(account, mapper.deserializeWithSecrets(mapper.serialize(account)));
  }

  @Test
  void canConvertJacksonizedAccountTypes() throws JsonProcessingException {
    var account = ValueAccount.builder().name("james").value("meowth").build();
    assertEquals(account, mapper.deserializeWithSecrets(mapper.serialize(account)));
  }

  @Test
  void canDecryptSecretUris() {
    var data = "{\"type\":\"test\",\"name\":\"bar\",\"password\":\"secret://noop?v=hunter2&k=v\"}";
    CredentialsDefinition account = assertDoesNotThrow(() -> mapper.deserializeWithSecrets(data));
    assertThat(account).isInstanceOf(TestAccount.class);
    assertThat(account.getName()).isEqualTo("bar");
    TestAccount testAccount = (TestAccount) account;
    org.assertj.core.api.Assertions.assertThat(testAccount.getData().get("password"))
        .isEqualTo("hunter2");
  }

  @Test
  void canDecryptEncryptedUris() {
    var data = "{\"type\":\"test\",\"name\":\"bar\",\"password\":\"encrypted:noop!v:hunter2\"}";
    CredentialsDefinition account = assertDoesNotThrow(() -> mapper.deserializeWithSecrets(data));
    assertThat(account).isInstanceOf(TestAccount.class);
    assertThat(account.getName()).isEqualTo("bar");
    TestAccount testAccount = (TestAccount) account;
    org.assertj.core.api.Assertions.assertThat(testAccount.getData().get("password"))
        .isEqualTo("hunter2");
  }

  @Configuration
  @ImportAutoConfiguration({JacksonAutoConfiguration.class, CredentialsAutoConfiguration.class})
  static class TestConfig {}
}
