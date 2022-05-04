/*
 * Copyright 2022 Apple Inc.
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

package com.netflix.spinnaker.kork.secrets.user;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.annotations.Beta;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps decrypted secret data to a corresponding {@link UserSecretData} using different encoding
 * formats. Encoding formats are specified by an {@link ObjectMapper}'s {@link
 * JsonFactory#getFormatName()} use case-insensitive string comparison. The type of user secret
 * being encoded is provided by metadata and must correspond to a UserSecretData class annotated
 * with {@link UserSecretType}.
 *
 * @see UserSecretData
 * @see UserSecretReference
 * @see UserSecretType
 */
@NonnullByDefault
@Beta
public class UserSecretMapper {
  private final Map<String, ObjectMapper> mappersByEncodingFormat =
      new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private final Map<String, Class<? extends UserSecretData>> userSecretTypes =
      new ConcurrentHashMap<>();

  public UserSecretMapper(
      Collection<ObjectMapper> mappers, Collection<Class<? extends UserSecretData>> types) {
    mappers.forEach(
        mapper -> mappersByEncodingFormat.put(mapper.getFactory().getFormatName(), mapper));
    types.forEach(
        type -> userSecretTypes.put(type.getAnnotation(UserSecretType.class).value(), type));
  }

  /**
   * Deserializes user secret data from a secret payload using the provided metadata options.
   *
   * @param input secret payload to deserialize
   * @param type type of secret being deserialized such as text or binary
   * @param encoding encoding of secret such as JSON, YAML, or CBOR
   * @return the deserialized user secret data
   */
  public UserSecretData deserialize(byte[] input, String type, String encoding) {
    var userSecretType = userSecretTypes.get(type);
    if (userSecretType == null) {
      throw new InvalidSecretFormatException(
          String.format(
              "Unsupported user secret type: %s. Known user secret types: %s",
              type, userSecretTypes.keySet()));
    }
    var mapper = mappersByEncodingFormat.get(encoding);
    if (mapper == null) {
      throw new InvalidSecretFormatException(
          String.format(
              "Unsupported user secret encoding: %s. Known encoding formats: %s",
              encoding, mappersByEncodingFormat.keySet()));
    }
    try {
      return mapper.readValue(input, userSecretType);
    } catch (IOException e) {
      throw new SecretDecryptionException(e);
    }
  }

  /**
   * Serializes user secret data using the provided encoding.
   *
   * @param secret user secret data to serialize to bytes
   * @param encoding encoding format to serialize data such as JSON, YAML, or CBOR
   * @return the encoded user secret data
   */
  public byte[] serialize(UserSecretData secret, String encoding) {
    var mapper = mappersByEncodingFormat.get(encoding);
    if (mapper == null) {
      throw new InvalidSecretFormatException(
          String.format(
              "Unsupported user secret encoding: %s. Known encoding formats: %s",
              encoding, mappersByEncodingFormat.keySet()));
    }
    try {
      return mapper.writeValueAsBytes(secret);
    } catch (JsonProcessingException e) {
      throw new SecretException(e);
    }
  }
}
