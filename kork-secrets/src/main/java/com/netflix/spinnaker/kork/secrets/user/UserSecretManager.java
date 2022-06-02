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

import com.netflix.spinnaker.kork.annotations.Beta;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretEngineRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Central component for obtaining user secrets by reference.
 *
 * @see UserSecretReference
 * @see UserSecret
 */
@Component
@RequiredArgsConstructor
@NonnullByDefault
@Beta
@CacheConfig(cacheManager = "userSecretsCacheManager", cacheNames = "userSecretsByRef")
public class UserSecretManager {
  private final SecretEngineRegistry registry;

  /**
   * Fetches and decrypts the given parsed user secret reference.
   *
   * @param reference parsed user secret reference to fetch
   * @return the decrypted user secret
   */
  @Cacheable
  public UserSecret getUserSecret(UserSecretReference reference) {
    String engineIdentifier = reference.getEngineIdentifier();
    SecretEngine engine = registry.getEngine(engineIdentifier);
    if (engine == null) {
      throw new SecretDecryptionException("Unknown secret engine identifier: " + engineIdentifier);
    }
    engine.validate(reference);
    return engine.decrypt(reference);
  }
}
