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
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.DescribeSecretRequest;
import com.amazonaws.services.secretsmanager.model.DescribeSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretException;
import com.netflix.spinnaker.kork.secrets.StandardSecretParameter;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadata;
import com.netflix.spinnaker.kork.secrets.user.UserSecretMetadataField;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.kork.secrets.user.UserSecretSerde;
import com.netflix.spinnaker.kork.secrets.user.UserSecretSerdeFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Secret engine using AWS Secrets Manager. Authentication is performed using the AWS managing
 * credentials and must have permission to perform {@code secretsmanager:DescribeSecret} and {@code
 * secretsmanager:GetSecretValue} actions on relevant secrets. The "describe secret" action is used
 * for {@link UserSecretMetadata} data which encodes said metadata as tags on the corresponding
 * secret. Tag keys correspond to {@link UserSecretMetadataField} constants, and the {@code
 * spinnaker:roles} tag should contain a comma-separated list of roles in its tag value (tags are
 * string:string key/value pairs, not arbitrary JSON). User secrets without a {@code
 * spinnaker:encoding} tag are assumed to be encoded as JSON to match existing typical usage of AWS
 * Secrets Manager, though other user secret encoding formats are still supported via that tag.
 */
@Component
public class SecretsManagerSecretEngine implements SecretEngine {
  protected static final String SECRET_NAME = "s";
  protected static final String SECRET_REGION = "r";
  protected static final String SECRET_KEY = StandardSecretParameter.KEY.getParameterName();

  private static final String IDENTIFIER = "secrets-manager";

  private final ObjectMapper mapper;
  private final UserSecretSerdeFactory userSecretSerdeFactory;
  private final SecretsManagerClientProvider clientProvider;
  private final Cache metadataCache;
  private final Cache dataCache;

  public SecretsManagerSecretEngine(
      ObjectMapper mapper,
      UserSecretSerdeFactory userSecretSerdeFactory,
      SecretsManagerClientProvider clientProvider,
      CacheManager cacheManager) {
    this.mapper = mapper;
    this.userSecretSerdeFactory = userSecretSerdeFactory;
    this.clientProvider = clientProvider;
    metadataCache = cacheManager.getCache(IDENTIFIER + "-tags");
    dataCache = cacheManager.getCache(IDENTIFIER);
  }

  @Override
  public String identifier() {
    return SecretsManagerSecretEngine.IDENTIFIER;
  }

  @Override
  public byte[] decrypt(EncryptedSecret encryptedSecret) {
    Map<String, String> params = encryptedSecret.getParams();
    CacheKey cacheKey = parseParameters(params);
    if (encryptedSecret.isEncryptedFile()) {
      return getSecretValue(cacheKey);
    } else {
      return getSecretValue(cacheKey, params.get(SECRET_KEY));
    }
  }

  @Override
  @NonNull
  public UserSecret decrypt(@NonNull UserSecretReference reference) {
    validate(reference);
    CacheKey cacheKey = parseParameters(reference.getParameters());
    UserSecretMetadata metadata = getUserSecretMetadata(cacheKey);
    UserSecretSerde serde = userSecretSerdeFactory.serdeFor(metadata);
    byte[] encodedData = getSecretValue(cacheKey);
    return serde.deserialize(encodedData, metadata);
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
    metadataCache.clear();
    dataCache.clear();
  }

  protected UserSecretMetadata getUserSecretMetadata(CacheKey cacheKey) {
    Map<String, String> tags;
    try {
      tags =
          metadataCache.get(
              cacheKey,
              () ->
                  getSecretDescription(cacheKey).getTags().stream()
                      .filter(tag -> tag.getKey().startsWith(UserSecretMetadataField.PREFIX))
                      .collect(Collectors.toMap(Tag::getKey, Tag::getValue)));
    } catch (Cache.ValueRetrievalException e) {
      throw new SecretException(e.getCause());
    }
    if (tags == null) {
      throw new SecretException("Unable to fetch secret metadata");
    }
    String type = tags.get(UserSecretMetadataField.TYPE.getTagKey());
    if (type == null) {
      throw new InvalidSecretFormatException(
          "No " + UserSecretMetadataField.TYPE.getTagKey() + " tag found in for " + cacheKey);
    }
    String encoding = tags.getOrDefault(UserSecretMetadataField.ENCODING.getTagKey(), "json");
    List<String> roles =
        Optional.ofNullable(tags.get(UserSecretMetadataField.ROLES.getTagKey())).stream()
            .flatMap(rolesValue -> Stream.of(rolesValue.split("\\s*,\\s*")))
            .collect(Collectors.toList());
    return UserSecretMetadata.builder().type(type).encoding(encoding).roles(roles).build();
  }

  protected DescribeSecretResult getSecretDescription(CacheKey cacheKey) {
    String region = cacheKey.getRegion();
    String secret = cacheKey.getSecret();
    AWSSecretsManager client = clientProvider.getClientForRegionAndSecret(region, secret);
    var request = new DescribeSecretRequest().withSecretId(secret);
    try {
      return client.describeSecret(request);
    } catch (AWSSecretsManagerException e) {
      throw new SecretException(
          String.format(
              "An error occurred when using AWS Secrets Manager to describe secret: [secretName: %s, secretRegion: %s]",
              secret, region),
          e);
    }
  }

  protected byte[] getSecretValue(CacheKey cacheKey, @Nullable String key) {
    byte[] value = getSecretValue(cacheKey);
    if (key == null) {
      return value;
    }
    try {
      Map<String, String> parsed = mapper.readerForMapOf(String.class).readValue(value);
      String extractedValue = parsed.get(key);
      if (extractedValue == null) {
        throw new SecretDecryptionException(
            String.format(
                "Specified key '%s' not found in AWS Secrets Manager for %s", key, cacheKey));
      }
      return extractedValue.getBytes(StandardCharsets.UTF_8);
    } catch (IOException | IllegalArgumentException e) {
      throw new SecretDecryptionException(
          String.format(
              "Failed to parse secret when using AWS Secrets Manager to fetch %s with key '%s'",
              cacheKey, key),
          e);
    }
  }

  protected byte[] getSecretValue(CacheKey cacheKey) {
    try {
      return dataCache.get(
          cacheKey, () -> fetchSecretValue(cacheKey.getRegion(), cacheKey.getSecret()));
    } catch (Cache.ValueRetrievalException e) {
      throw new SecretDecryptionException(e.getCause());
    }
  }

  protected byte[] fetchSecretValue(String region, String secret) {
    AWSSecretsManager client = clientProvider.getClientForRegionAndSecret(region, secret);
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secret);

    GetSecretValueResult result;
    try {
      result = client.getSecretValue(getSecretValueRequest);
    } catch (AWSSecretsManagerException e) {
      throw new SecretException(
          String.format(
              "An error occurred when using AWS Secrets Manager to fetch: [secretName: %s, secretRegion: %s]",
              secret, region),
          e);
    }
    ByteBuffer secretBinary = result.getSecretBinary();
    if (secretBinary != null) {
      return toByteArray(secretBinary);
    }
    return result.getSecretString().getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] toByteArray(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }

  private static CacheKey parseParameters(Map<String, String> parameters) {
    return new CacheKey(parameters.get(SECRET_REGION), parameters.get(SECRET_NAME));
  }

  @Value
  public static class CacheKey {
    String region;
    String secret;
  }
}
