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

package com.netflix.spinnaker.credentials.validator;

import com.netflix.spinnaker.kork.exceptions.HasAdditionalAttributes;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretError;
import com.netflix.spinnaker.kork.secrets.SecretErrorCode;
import com.netflix.spinnaker.kork.secrets.SecretException;
import com.netflix.spinnaker.kork.secrets.SecretReferenceValidator;
import com.netflix.spinnaker.kork.secrets.StandardSecretParameter;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.kork.secrets.user.UserSecretService;
import com.netflix.spinnaker.security.SpinnakerAuthorities;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Log4j2
@Component
@RequiredArgsConstructor
public class DefaultSecretReferenceValidator implements SecretReferenceValidator {
  private final UserSecretService secretService;

  @Override
  public Optional<SecretError> validateUserSecretReference(String uri) {
    log.debug("Validating user secret reference '{}'", uri);
    try {
      UserSecretReference ref = UserSecretReference.parse(uri);
      UserSecret secret = secretService.getUserSecret(ref);
      String key = ref.getParameters().get(StandardSecretParameter.KEY.getParameterName());
      if (key != null) {
        try {
          secret.getSecretString(key);
        } catch (NoSuchElementException e) {
          return Optional.of(SecretErrorCode.MISSING_USER_SECRET_DATA_KEY);
        }
      }
    } catch (AccessDeniedException e) {
      log.info("Access denied to user secret reference {}", uri);
      return Optional.of(SecretErrorCode.DENIED_ACCESS_TO_USER_SECRET);
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (e instanceof SecretError) {
        SecretError error = (SecretError) e;
        log.info("Secret validation error for string '{}': {}", uri, message, e);
        return Optional.of(error);
      }
      log.info("Unable to decrypt secret '{}'", uri, e);
      return Optional.of(translateException(e, SecretErrorCode.USER_SECRET_DECRYPTION_FAILURE));
    }
    return Optional.empty();
  }

  @Override
  public Optional<SecretError> validateExternalSecretReference(String uri) {
    log.debug("Validating external secret reference '{}'", uri);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!SpinnakerAuthorities.isAdmin(authentication)) {
      return Optional.of(SecretErrorCode.DENIED_ACCESS_TO_EXTERNAL_SECRET);
    }
    try {
      EncryptedSecret ref = EncryptedSecret.parse(uri);
      if (ref == null) {
        log.info("Unable to parse external secret URI string '{}'", uri);
        return Optional.of(SecretErrorCode.INVALID_EXTERNAL_SECRET_URI);
      }
      secretService.checkExternalSecret(ref);
    } catch (InvalidSecretFormatException e) {
      log.info("Invalid external secret URI format for string '{}'", uri, e);
      return Optional.of(translateException(e, SecretErrorCode.INVALID_EXTERNAL_SECRET_URI));
    } catch (SecretException e) {
      log.info("Unable to decrypt external secret reference '{}'", uri, e);
      return Optional.of(translateException(e, SecretErrorCode.EXTERNAL_SECRET_DECRYPTION_FAILURE));
    }
    return Optional.empty();
  }

  private static GenericSecretError translateException(
      Throwable throwable, SecretErrorCode errorCode) {
    String message = throwable.getMessage();
    if (!StringUtils.hasText(message)) {
      message = errorCode.getMessage();
    }
    var builder = GenericSecretError.builder().message(message).errorCode(errorCode.getErrorCode());
    if (throwable instanceof HasAdditionalAttributes) {
      builder.additionalAttributes(((HasAdditionalAttributes) throwable).getAdditionalAttributes());
    }
    return builder.build();
  }

  @Getter
  @Builder
  private static class GenericSecretError implements SecretError {
    private final String errorCode;
    private final String message;
    @Builder.Default private final Map<String, Object> additionalAttributes = Map.of();
  }
}
