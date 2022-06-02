/*
 * Copyright 2020 Nike, Inc.
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
package com.netflix.spinnaker.kork.secrets.engines;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.DescribeSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretException;
import com.netflix.spinnaker.kork.secrets.user.DefaultUserSecretSerde;
import com.netflix.spinnaker.kork.secrets.user.OpaqueUserSecretData;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretData;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadata;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadataField;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.kork.secrets.user.UserSecretSerde;
import com.netflix.spinnaker.kork.secrets.user.UserSecretSerdeFactory;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SecretsManagerSecretEngineTest {
  private SecretsManagerSecretEngine secretsManagerSecretEngine;

  @Mock private AWSSecretsManager awsSecretsManager;

  private UserSecretSerde userSecretSerde;

  private GetSecretValueResult kvSecretValue =
      new GetSecretValueResult().withSecretString("{\"password\":\"hunter2\"}");
  private GetSecretValueResult plaintextSecretValue =
      new GetSecretValueResult().withSecretString("letmein");
  private GetSecretValueResult binarySecretValue =
      new GetSecretValueResult().withSecretBinary(ByteBuffer.wrap("i'm binary".getBytes()));
  private GetSecretValueResult secretStringFileValue =
      new GetSecretValueResult().withSecretString("BEGIN RSA PRIVATE KEY");

  @Before
  public void setup() {
    ObjectMapper mapper = new ObjectMapper();
    List<ObjectMapper> mappers = List.of(mapper);
    userSecretSerde = new DefaultUserSecretSerde(mappers, List.of(OpaqueUserSecretData.class));
    UserSecretSerdeFactory userSecretSerdeFactory =
        new UserSecretSerdeFactory(List.of(userSecretSerde));
    secretsManagerSecretEngine =
        new SecretsManagerSecretEngine(
            mapper, userSecretSerdeFactory, (ignore, ignored) -> awsSecretsManager);
  }

  @Test
  public void decryptStringWithKey() {
    EncryptedSecret kvSecret =
        EncryptedSecret.parse("encrypted:secrets-manager!r:us-west-2!s:test-secret!k:password");
    assertNotNull(kvSecret);
    when(awsSecretsManager.getSecretValue(any())).thenReturn(kvSecretValue);
    assertArrayEquals("hunter2".getBytes(), secretsManagerSecretEngine.decrypt(kvSecret));
  }

  @Test
  public void decryptStringWithoutKey() {
    EncryptedSecret plaintextSecret =
        EncryptedSecret.parse("encrypted:secrets-manager!r:us-west-2!s:test-secret");
    assertNotNull(plaintextSecret);
    when(awsSecretsManager.getSecretValue(any())).thenReturn(plaintextSecretValue);
    assertArrayEquals("letmein".getBytes(), secretsManagerSecretEngine.decrypt(plaintextSecret));
  }

  @Test
  public void decryptFileWithKey() {
    EncryptedSecret kvSecret =
        EncryptedSecret.parse("encryptedFile:secrets-manager!r:us-west-2!s:private-key!k:password");
    assertNotNull(kvSecret);
    assertThrows(
        InvalidSecretFormatException.class, () -> secretsManagerSecretEngine.validate(kvSecret));
  }

  @Test
  public void decryptSecretStringAsFile() {
    EncryptedSecret secretStringFile =
        EncryptedSecret.parse("encryptedFile:secrets-manager!r:us-west-2!s:private-key");
    assertNotNull(secretStringFile);
    when(awsSecretsManager.getSecretValue(any())).thenReturn(secretStringFileValue);
    assertArrayEquals(
        "BEGIN RSA PRIVATE KEY".getBytes(), secretsManagerSecretEngine.decrypt(secretStringFile));
  }

  @Test
  public void decryptSecretBinaryAsFile() {
    EncryptedSecret secretBinaryFile =
        EncryptedSecret.parse("encryptedFile:secrets-manager!r:us-west-2!s:private-key");
    assertNotNull(secretBinaryFile);
    when(awsSecretsManager.getSecretValue(any())).thenReturn(binarySecretValue);
    assertArrayEquals(
        "i'm binary".getBytes(), secretsManagerSecretEngine.decrypt(secretBinaryFile));
  }

  @Test
  public void decryptStringWithBinaryResult() {
    EncryptedSecret kvSecret =
        EncryptedSecret.parse("encrypted:secrets-manager!r:us-west-2!s:test-secret!k:password");
    assertNotNull(kvSecret);
    when(awsSecretsManager.getSecretValue(any())).thenReturn(binarySecretValue);
    assertThrows(SecretException.class, () -> secretsManagerSecretEngine.decrypt(kvSecret));
  }

  @Test
  public void decryptJsonUserSecret() {
    DescribeSecretResult description =
        new DescribeSecretResult()
            .withTags(
                new Tag().withKey(UserSecretMetadataField.TYPE.getTagKey()).withValue("opaque"),
                new Tag().withKey(UserSecretMetadataField.ROLES.getTagKey()).withValue("a, b, c"));
    when(awsSecretsManager.describeSecret(any())).thenReturn(description);

    UserSecretData data = new OpaqueUserSecretData(Map.of("password", "hunter2"));
    UserSecretMetadata metadata =
        UserSecretMetadata.builder()
            .type("opaque")
            .encoding("json")
            .roles(List.of("a", "b", "c"))
            .build();
    byte[] secretBytes = userSecretSerde.serialize(data, metadata);
    GetSecretValueResult stubResult =
        new GetSecretValueResult().withSecretBinary(ByteBuffer.wrap(secretBytes));
    when(awsSecretsManager.getSecretValue(any())).thenReturn(stubResult);

    UserSecretReference reference =
        UserSecretReference.parse("secret://secrets-manager?r=us-west-2&s=private-key");
    UserSecret secret = secretsManagerSecretEngine.decrypt(reference);
    assertEquals("hunter2", secret.getSecretString("password"));
    assertEquals(List.of("a", "b", "c"), secret.getRoles());
  }
}
