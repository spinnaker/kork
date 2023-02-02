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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionSecretManager;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import com.netflix.spinnaker.kork.secrets.SecretErrorCode;
import com.netflix.spinnaker.kork.secrets.user.UserSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.security.AccessControlled;
import java.beans.PropertyDescriptor;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validates that {@link UserSecretReference} strings point to valid user secrets that the current
 * user is authorized to use. When invoked as a Spring {@link Validator}, this should validate that
 * the current user can read referenced secrets. When invoked as a {@link
 * CredentialsDefinitionValidator}, though, this should validate that the current user is allowed to
 * save credentials that reference secrets. This is primarily relevant when using {@link
 * EncryptedSecret} URIs which may only be configured by a Spinnaker administrator.
 */
@Component
@NonnullByDefault
@RequiredArgsConstructor
public class UserSecretsValidator implements CredentialsDefinitionValidator, Validator {
  private final CredentialsDefinitionSecretManager secretManager;

  @Override
  public boolean supports(Class<?> clazz) {
    return CredentialsDefinition.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(target);
    validate(bean, errors, authentication, false);
  }

  @Override
  public void validate(
      CredentialsDefinition definition, Errors errors, Authentication authentication) {
    BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(definition);
    validate(bean, errors, authentication, true);
  }

  private void validate(
      BeanWrapper bean, Errors errors, Authentication authentication, boolean saving) {
    // when saving an account, only admins can use external secrets
    boolean denyUserSecrets = saving && !AccessControlled.isAdmin(authentication);
    Stream.of(bean.getPropertyDescriptors())
        .filter(descriptor -> descriptor.getPropertyType() == String.class)
        .map(PropertyDescriptor::getName)
        .forEach(
            propertyName -> {
              String value = (String) bean.getPropertyValue(propertyName);
              if (value == null) {
                return;
              }
              if (UserSecretReference.isUserSecret(value)) {
                try {
                  UserSecretReference ref = UserSecretReference.parse(value);
                  UserSecret secret = secretManager.getUserSecret(ref);
                  if (!secretManager.canReadUserSecret(authentication, secret)) {
                    errors.rejectValue(
                        propertyName,
                        SecretErrorCode.DENIED_ACCESS_TO_USER_SECRET.getErrorCode(),
                        "Access denied to user secret");
                  }
                } catch (InvalidSecretFormatException e) {
                  errors.rejectValue(
                      propertyName,
                      SecretErrorCode.INVALID_USER_SECRET_URI.getErrorCode(),
                      e.getMessage());
                } catch (SecretDecryptionException e) {
                  errors.rejectValue(
                      propertyName,
                      SecretErrorCode.USER_SECRET_DECRYPTION_FAILURE.getErrorCode(),
                      e.getMessage());
                }
              }
              if (EncryptedSecret.isEncryptedSecret(value)) {
                if (denyUserSecrets) {
                  errors.rejectValue(
                      propertyName,
                      SecretErrorCode.DENIED_ACCESS_TO_EXTERNAL_SECRET.getErrorCode(),
                      "Only admins can define external secrets");
                  return;
                }
                try {
                  EncryptedSecret ref = EncryptedSecret.parse(value);
                  if (ref == null) {
                    errors.rejectValue(
                        propertyName,
                        SecretErrorCode.INVALID_EXTERNAL_SECRET_URI.getErrorCode(),
                        "Unable to parse external secret URI");
                  } else {
                    // try to fetch the secret
                    secretManager.getExternalSecretString(ref);
                  }
                } catch (InvalidSecretFormatException e) {
                  errors.rejectValue(
                      propertyName,
                      SecretErrorCode.INVALID_EXTERNAL_SECRET_URI.getErrorCode(),
                      e.getMessage());
                } catch (SecretDecryptionException e) {
                  errors.rejectValue(
                      propertyName,
                      SecretErrorCode.EXTERNAL_SECRET_DECRYPTION_FAILURE.getErrorCode(),
                      e.getMessage());
                }
              }
            });
  }
}
