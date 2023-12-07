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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionValidator;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.kork.api.exceptions.ConstraintViolation;
import com.netflix.spinnaker.kork.api.exceptions.ConstraintViolationContext;
import com.netflix.spinnaker.kork.secrets.SecretErrorCode;
import com.netflix.spinnaker.kork.secrets.SecretReferenceValidator;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validates that {@link UserSecretReference} strings point to valid user secrets that the current
 * user is authorized to use.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class UserSecretsValidator implements CredentialsDefinitionValidator, Validator {

  private final SecretReferenceValidator secretReferenceValidator;
  private final ObjectProvider<ConstraintViolationContext> constraintViolationContextProvider;

  @Override
  public boolean supports(Class<?> clazz) {
    return CredentialsDefinition.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    log.debug("Validating user secrets in account");
    BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(target);
    forEachNonNullStringProperty(
        bean,
        (name, value) ->
            secretReferenceValidator
                .validate(value)
                .ifPresent(
                    error -> {
                      if (error != SecretErrorCode.DENIED_ACCESS_TO_EXTERNAL_SECRET) {
                        errors.rejectValue(name, error.getErrorCode(), error.getMessage());
                        constraintViolationContextProvider.ifAvailable(
                            details ->
                                details.addViolation(
                                    ConstraintViolation.builder()
                                        .message(error.getMessage())
                                        .errorCode(error.getErrorCode())
                                        .path(name)
                                        .validatedObject(target)
                                        .invalidValue(value)
                                        .additionalAttributes(error.getAdditionalAttributes())
                                        .build()));
                      }
                    }));
  }

  @Override
  public boolean isValid(CredentialsDefinition value, ConstraintValidatorContext context) {
    log.debug("Validating user secrets in account");
    BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(value);
    AtomicBoolean invalid = new AtomicBoolean();
    forEachNonNullStringProperty(
        bean,
        (propertyName, propertyValue) ->
            secretReferenceValidator
                .validate(propertyValue)
                .ifPresent(
                    error -> {
                      invalid.set(true);
                      context
                          .buildConstraintViolationWithTemplate(error.getErrorCode())
                          .addPropertyNode(propertyName)
                          .addConstraintViolation();
                      constraintViolationContextProvider.ifAvailable(
                          details ->
                              details.addViolation(
                                  ConstraintViolation.builder()
                                      .message(error.getMessage())
                                      .errorCode(error.getErrorCode())
                                      .path(propertyName)
                                      .validatedObject(value)
                                      .invalidValue(propertyValue)
                                      .additionalAttributes(error.getAdditionalAttributes())
                                      .build()));
                    }));
    return !invalid.get();
  }

  private void forEachNonNullStringProperty(BeanWrapper bean, BiConsumer<String, String> consumer) {
    // as CredentialsDefinitionMapper only considers top-level string properties for injecting
    // secrets, we only need to consider the top-level bean properties that are string-like along
    // with the special case for @JsonAnyGetter-annotated properties returning a Map as those may
    // also have entries whose values are string-like and can have secrets injected
    for (PropertyDescriptor descriptor : bean.getPropertyDescriptors()) {
      String propertyName = descriptor.getName();
      Object propertyValue = bean.getPropertyValue(propertyName);
      if (propertyValue == null) {
        continue;
      }
      Method readMethod = descriptor.getReadMethod();
      if (AnnotationUtils.findAnnotation(readMethod, JsonAnyGetter.class) != null
          && Map.class.isAssignableFrom(readMethod.getReturnType())) {
        ((Map<?, ?>) propertyValue)
            .forEach(
                (key, mapValue) -> {
                  String value = getStringLikeValue(mapValue);
                  if (value != null) {
                    String fullKey = propertyName + '.' + key;
                    consumer.accept(fullKey, value);
                  }
                });
      } else {
        String value = getStringLikeValue(propertyValue);
        if (value != null) {
          consumer.accept(propertyName, value);
        }
      }
    }
  }

  @Nullable
  private static String getStringLikeValue(@Nullable Object value) {
    if (value instanceof CharSequence) {
      return ((CharSequence) value).toString();
    }
    if (value instanceof Optional<?>) {
      Object containedValue = ((Optional<?>) value).orElse(null);
      if (containedValue instanceof CharSequence) {
        return ((CharSequence) containedValue).toString();
      }
    }
    return null;
  }
}
