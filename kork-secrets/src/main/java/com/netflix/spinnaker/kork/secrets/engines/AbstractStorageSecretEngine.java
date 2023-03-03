/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.Value;
import org.springframework.cache.Cache;

public abstract class AbstractStorageSecretEngine implements SecretEngine {
  protected static final String STORAGE_BUCKET = "b";
  protected static final String STORAGE_REGION = "r";
  protected static final String STORAGE_FILE_URI = "f";
  protected static final String STORAGE_PROP_KEY = "k";

  private static final Pattern KEY_PATH_SEPARATOR = Pattern.compile("\\.");

  protected final Cache cache;
  protected final YAMLMapper yamlMapper = YAMLMapper.builder().deactivateDefaultTyping().build();

  protected AbstractStorageSecretEngine(Cache cache) {
    this.cache = cache;
  }

  public byte[] decrypt(EncryptedSecret encryptedSecret) {
    Map<String, String> params = encryptedSecret.getParams();
    String bucket = params.get(STORAGE_BUCKET);
    String region = params.get(STORAGE_REGION);
    String fileUri = params.get(STORAGE_FILE_URI);
    String key = params.get(STORAGE_PROP_KEY);
    CacheKey cacheKey = new CacheKey(bucket, region, fileUri);

    try {
      return cache.get(cacheKey, () -> getSecretValue(cacheKey, key));
    } catch (Cache.ValueRetrievalException e) {
      Throwable cause = e.getCause();
      throw cause instanceof SecretException
          ? (SecretException) cause
          : new SecretDecryptionException(cause);
    }
  }

  public void validate(EncryptedSecret encryptedSecret) throws InvalidSecretFormatException {
    Set<String> paramNames = encryptedSecret.getParams().keySet();
    if (!paramNames.contains(STORAGE_BUCKET)) {
      throw new InvalidSecretFormatException(
          "Storage bucket parameter is missing (" + STORAGE_BUCKET + "=...)");
    }
    if (!paramNames.contains(STORAGE_REGION)) {
      throw new InvalidSecretFormatException(
          "Storage region parameter is missing (" + STORAGE_REGION + "=...)");
    }
    if (!paramNames.contains(STORAGE_FILE_URI)) {
      throw new InvalidSecretFormatException(
          "Storage file parameter is missing (" + STORAGE_FILE_URI + "=...)");
    }
  }

  public EncryptedSecret encrypt(String secretToEncrypt) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("This operation is not supported");
  }

  protected abstract InputStream downloadRemoteFile(CacheKey cacheKey) throws IOException;

  protected byte[] getSecretValue(CacheKey cacheKey, @Nullable String key) {
    try (InputStream remoteFile = downloadRemoteFile(cacheKey)) {
      if (key == null) {
        return remoteFile.readAllBytes();
      }
      JsonNode node = yamlMapper.readTree(remoteFile);
      for (String path : KEY_PATH_SEPARATOR.split(key)) {
        if (node.isObject()) {
          node = node.path(path);
        } else if (node.isArray()) {
          node = node.path(Integer.parseInt(path));
        } else {
          throw new SecretDecryptionException(
              String.format("Invalid secret key specified: %s", key));
        }
      }
      if (node.isValueNode()) {
        return node.asText().getBytes(StandardCharsets.UTF_8);
      }
      throw new SecretDecryptionException(String.format("Invalid secret key specified: %s", key));
    } catch (IOException e) {
      throw new SecretDecryptionException(e);
    }
  }

  public void clearCache() {
    cache.clear();
  }

  @Value
  @NonnullByDefault
  public static class CacheKey {
    String bucket;
    String region;
    String file;
  }
}
