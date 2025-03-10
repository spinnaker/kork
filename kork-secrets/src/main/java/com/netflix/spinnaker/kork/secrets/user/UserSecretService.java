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

package com.netflix.spinnaker.kork.secrets.user;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.StandardSecretParameter;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NonnullByDefault
public class UserSecretService {
  private final UserSecretManager secretManager;
  private final Map<String, Set<UserSecretReference>> resourceIdsWithUserSecrets =
      new ConcurrentHashMap<>();

  @PostAuthorize("hasPermission(returnObject, 'READ')")
  public UserSecret getUserSecret(UserSecretReference ref) {
    return secretManager.getUserSecret(ref);
  }

  /**
   * Gets a user secret string value for the given resource id. User secret references are tracked
   * through this method to support time-of-use access control checks for resources after they've
   * been loaded by the system.
   *
   * @param ref parsed user secret reference to decrypt
   * @param resourceId id of resource referencing the user secret
   * @return the contents of the requested user secret string
   */
  public String getUserSecretStringForResource(UserSecretReference ref, String resourceId) {
    var secret = secretManager.getUserSecret(ref);
    resourceIdsWithUserSecrets
        .computeIfAbsent(resourceId, ignored -> ConcurrentHashMap.newKeySet())
        .add(ref);
    var params = ref.getParameters();
    String key = params.getOrDefault(StandardSecretParameter.KEY.getParameterName(), "");
    try {
      return secret.getSecretString(key);
    } catch (NoSuchElementException e) {
      throw new SecretDecryptionException(e);
    }
  }

  /**
   * Checks whether the provided resource id is being tracked for user secrets. Resources with no
   * user secrets or that do not exist should return {@code false}.
   */
  public boolean isTrackingUserSecretsForResource(String resourceId) {
    return resourceIdsWithUserSecrets.containsKey(resourceId);
  }

  /** Performs an action for all tracked user secrets of the provided resource id. */
  public void forEachUserSecretOfResource(
      String resourceId, BiConsumer<UserSecretReference, UserSecret> action) {
    resourceIdsWithUserSecrets
        .getOrDefault(resourceId, Set.of())
        .forEach(ref -> action.accept(ref, secretManager.getUserSecret(ref)));
  }

  /**
   * Attempts to fetch and decrypt the given parsed external secret reference. If this is unable to
   * fetch the secret, then this will throw an exception.
   *
   * @param reference parsed external secret reference to fetch
   * @throws SecretDecryptionException if the external secret does not have a corresponding secret
   *     engine or cannot be fetched
   * @throws InvalidSecretFormatException if the external secret reference is invalid
   */
  public void checkExternalSecret(EncryptedSecret reference) {
    secretManager.getExternalSecret(reference);
  }

  /**
   * Fetches and decrypts the given parsed external secret reference encoded as a string. External
   * secrets are secrets available through {@link EncryptedSecret} URIs.
   *
   * @param reference parsed external secret reference to fetch
   * @return the decrypted external secret string
   * @throws SecretDecryptionException if the external secret does not have a corresponding secret
   *     engine or cannot be fetched
   * @throws InvalidSecretFormatException if the external secret reference is invalid
   */
  public String getExternalSecretString(EncryptedSecret reference) {
    return secretManager.getExternalSecretString(reference);
  }
}
