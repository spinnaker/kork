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

package com.netflix.spinnaker.credentials.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.types.CredentialsDefinitionTypesConfiguration;
import io.spinnaker.test.security.ValueAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
    properties = "credentials.types.additional-scan-packages=io.spinnaker.test.security")
@AutoConfigureJson
class ValidateCredentialsMutationTest {
  @Autowired ObjectMapper mapper;

  @Test
  void ensureSerializationAndDeserializationWorkAsExpected() throws Exception {
    var credentials = ValueAccount.builder().name("account").value("value").build();
    assertThat(
            mapper.readValue(mapper.writeValueAsString(credentials), CredentialsDefinition.class))
        .isInstanceOf(ValueAccount.class)
        .isEqualTo(credentials);

    var create = new ValidateCredentialsMutation.Create(credentials);
    var deserialized =
        mapper.readValue(mapper.writeValueAsString(create), ValidateCredentialsMutation.class);
    assertThat(deserialized)
        .isInstanceOf(ValidateCredentialsMutation.Create.class)
        .returns(
            credentials,
            mutation -> ((ValidateCredentialsMutation.Create) mutation).getCredentials());

    var manualSerialization =
        "{\"type\":\"save\",\"credentials\":{\"type\":\"value\",\"name\":\"account\",\"value\":\"value\"}}";
    deserialized = mapper.readValue(manualSerialization, ValidateCredentialsMutation.class);
    assertThat(deserialized)
        .isInstanceOf(ValidateCredentialsMutation.Save.class)
        .returns(
            credentials,
            mutation -> ((ValidateCredentialsMutation.Save) mutation).getCredentials().get(0));
  }

  @Configuration
  @ImportAutoConfiguration(CredentialsDefinitionTypesConfiguration.class)
  static class Config {}
}
