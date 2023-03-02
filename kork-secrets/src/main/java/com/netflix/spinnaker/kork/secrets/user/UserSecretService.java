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

package com.netflix.spinnaker.kork.secrets.user;

import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Component;

/** Provides secure access to user secrets based on the current authenticated user. */
@Component
@RequiredArgsConstructor
public class UserSecretService {
  private final UserSecretManager secretManager;

  /**
   * Gets a user secret by reference if the current user has authorization to read the secret.
   *
   * @param ref parsed user secret reference to fetch
   * @return the decrypted user secret
   * @throws SecretDecryptionException if the secret reference does not have a corresponding secret
   *     engine or cannot be fetched
   * @throws InvalidSecretFormatException if the secret is missing its {@link UserSecretMetadata} or
   *     said metadata cannot be parsed
   * @throws UnsupportedOperationException if the underlying {@link SecretEngine} does not support
   *     user secrets
   * @throws AccessDeniedException if the current user does not have permission to read the secret
   */
  @PostAuthorize("hasPermission(returnObject, 'READ')")
  public UserSecret getUserSecret(UserSecretReference ref) {
    return secretManager.getUserSecret(ref);
  }
}
