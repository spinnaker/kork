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

package com.netflix.spinnaker.kork.secrets;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import java.util.Optional;

/**
 * Validation strategy for checking strings for possible secret references and validating that the
 * current authenticated user (if available) is allowed to use it.
 */
@NonnullByDefault
public interface SecretReferenceValidator {
  /**
   * Checks the given string to see if it is a valid {@link UserSecretReference} or {@link
   * EncryptedSecret} URI and performs validation on the referenced secret. If the given string does
   * not match {@link UserSecretReference#isUserSecret(String)} or {@link
   * EncryptedSecret#isEncryptedSecret(String)}, then this will not validate the string any further.
   *
   * @param secretReference string possibly containing a secret reference URI
   * @return an optional containing the {@link SecretError} if invalid or an empty optional
   *     otherwise
   */
  default Optional<SecretError> validate(String secretReference) {
    if (UserSecretReference.isUserSecret(secretReference)) {
      return validateUserSecretReference(secretReference);
    }
    if (EncryptedSecret.isEncryptedSecret(secretReference)) {
      return validateExternalSecretReference(secretReference);
    }
    return Optional.empty();
  }

  /**
   * Checks the given string to see if it is a valid {@link UserSecretReference} and that its
   * referenced {@link UserSecret} is valid.
   *
   * @param uri secret URI to validate
   * @return a specific error code for a validation error or empty
   */
  Optional<SecretError> validateUserSecretReference(String uri);

  /**
   * Checks the given string to see if it is a valid {@link EncryptedSecret} and that its referenced
   * secret data can be fetched.
   *
   * @param uri external secret URI to validate
   * @return a specific error code for a validation error or empty
   */
  Optional<SecretError> validateExternalSecretReference(String uri);
}
