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

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.DescribeSecretRequest;
import com.amazonaws.services.secretsmanager.model.DescribeSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretException;
import com.netflix.spinnaker.kork.secrets.StandardSecretParameter;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretData;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMapper;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadata;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class SecretsManagerSecretEngine implements SecretEngine {
  protected static final String SECRET_NAME = "s";
  protected static final String SECRET_REGION = "r";
  protected static final String SECRET_KEY = StandardSecretParameter.KEY.getParameterName();

  private static String IDENTIFIER = "secrets-manager";

  private Map<String, Map<String, String>> cache = new HashMap<>();
  private UserSecretMapper userSecretMapper;
  private static final ObjectMapper mapper = new ObjectMapper();

  @Autowired
  void setUserSecretMapper(UserSecretMapper userSecretMapper) {
    this.userSecretMapper = userSecretMapper;
  }

  @Override
  public String identifier() {
    return SecretsManagerSecretEngine.IDENTIFIER;
  }

  @Override
  public byte[] decrypt(EncryptedSecret encryptedSecret) {
    String secretRegion = encryptedSecret.getParams().get(SECRET_REGION);
    String secretName = encryptedSecret.getParams().get(SECRET_NAME);
    String secretKey = encryptedSecret.getParams().get(SECRET_KEY);

    if (encryptedSecret.isEncryptedFile()) {
      GetSecretValueResult secretFileValue = getSecretValue(secretRegion, secretName);
      if (secretFileValue.getSecretBinary() != null) {
        return secretFileValue.getSecretBinary().array();
      } else {
        return secretFileValue.getSecretString().getBytes();
      }
    } else if (secretKey != null) {
      return getSecretString(secretRegion, secretName, secretKey);
    } else {
      return getSecretString(secretRegion, secretName);
    }
  }

  @Override
  @NonNull
  public UserSecret decrypt(@NonNull UserSecretReference reference) {
    validate(reference);
    Map<String, String> parameters = reference.getParameters();
    String secretRegion = parameters.get(SECRET_REGION);
    String secretName = parameters.get(SECRET_NAME);
    Map<String, String> tags =
        getSecretDescription(secretRegion, secretName).getTags().stream()
            .filter(tag -> tag.getKey().startsWith("spinnaker:"))
            .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    String type = tags.get("spinnaker:type");
    if (type == null) {
      throw new InvalidSecretFormatException("No spinnaker:type tag found for " + reference);
    }
    String encoding = tags.getOrDefault("spinnaker:encoding", "json");
    List<String> roles =
        Optional.ofNullable(tags.get("spinnaker:roles")).stream()
            .flatMap(rolesValue -> Stream.of(rolesValue.split("\\s*,\\s*")))
            .collect(Collectors.toList());
    UserSecretMetadata metadata =
        UserSecretMetadata.builder().type(type).encoding(encoding).roles(roles).build();
    GetSecretValueResult secretValue = getSecretValue(secretRegion, secretName);
    ByteBuffer secretBinary = secretValue.getSecretBinary();
    byte[] encodedData =
        secretBinary != null
            ? secretBinary.array()
            : secretValue.getSecretString().getBytes(StandardCharsets.UTF_8);
    UserSecretData data = userSecretMapper.deserialize(encodedData, type, encoding);
    return UserSecret.builder().metadata(metadata).data(data).build();
  }

  @Override
  public void validate(EncryptedSecret encryptedSecret) {
    Set<String> paramNames = encryptedSecret.getParams().keySet();
    if (!paramNames.contains(SECRET_NAME)) {
      throw new InvalidSecretFormatException(
          "Secret name parameter is missing (" + SECRET_NAME + "=...)");
    }
    if (!paramNames.contains(SECRET_REGION)) {
      throw new InvalidSecretFormatException(
          "Secret region parameter is missing (" + SECRET_REGION + "=...)");
    }
    if (encryptedSecret.isEncryptedFile() && paramNames.contains(SECRET_KEY)) {
      throw new InvalidSecretFormatException("Encrypted file should not specify key");
    }
  }

  @Override
  public void validate(@NonNull UserSecretReference reference) {
    Set<String> paramNames = reference.getParameters().keySet();
    if (!paramNames.contains(SECRET_NAME)) {
      throw new InvalidSecretFormatException(
          "Secret name parameter is missing (" + SECRET_NAME + "=...)");
    }
    if (!paramNames.contains(SECRET_REGION)) {
      throw new InvalidSecretFormatException(
          "Secret region parameter is missing (" + SECRET_REGION + "=...)");
    }
  }

  @Override
  public void clearCache() {
    cache.clear();
  }

  protected DescribeSecretResult getSecretDescription(String secretRegion, String secretName) {
    AWSSecretsManager client =
        AWSSecretsManagerClientBuilder.standard().withRegion(secretRegion).build();
    var request = new DescribeSecretRequest().withSecretId(secretName);
    try {
      return client.describeSecret(request);
    } catch (AWSSecretsManagerException e) {
      throw new SecretException(
          String.format(
              "An error occurred when using AWS Secrets Manager to describe secret: [secretName: %s, secretRegion: %s]",
              secretName, secretRegion),
          e);
    }
  }

  protected GetSecretValueResult getSecretValue(String secretRegion, String secretName) {
    AWSSecretsManager client =
        AWSSecretsManagerClientBuilder.standard().withRegion(secretRegion).build();

    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(secretName);

    try {
      return client.getSecretValue(getSecretValueRequest);
    } catch (AWSSecretsManagerException e) {
      throw new SecretException(
          String.format(
              "An error occurred when using AWS Secrets Manager to fetch: [secretName: %s, secretRegion: %s]",
              secretName, secretRegion),
          e);
    }
  }

  private byte[] getSecretString(String secretRegion, String secretName, String secretKey) {
    if (!cache.containsKey(secretName)) {
      String secretString = getSecretValue(secretRegion, secretName).getSecretString();
      try {
        Map<String, String> map = mapper.readerForMapOf(String.class).readValue(secretString);
        cache.put(secretName, map);
      } catch (JsonProcessingException | IllegalArgumentException e) {
        throw new SecretException(
            String.format(
                "Failed to parse secret when using AWS Secrets Manager to fetch: [secretName: %s, secretRegion: %s, secretKey: %s]",
                secretName, secretRegion, secretKey),
            e);
      }
    }
    return Optional.ofNullable(cache.get(secretName).get(secretKey))
        .orElseThrow(
            () ->
                new SecretException(
                    String.format(
                        "Specified key not found in AWS Secrets Manager: [secretName: %s, secretRegion: %s, secretKey: %s]",
                        secretName, secretRegion, secretKey)))
        .getBytes();
  }

  private byte[] getSecretString(String secretRegion, String secretName) {
    return getSecretValue(secretRegion, secretName).getSecretString().getBytes();
  }
}
