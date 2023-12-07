/*
 * Copyright 2023 Apple, Inc.
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

package com.netflix.spinnaker.credentials.service;

import com.netflix.spinnaker.credentials.Credentials;
import com.netflix.spinnaker.credentials.CredentialsRepository;
import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionValidator;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.kork.web.exceptions.InvalidRequestException;
import com.netflix.spinnaker.kork.web.exceptions.ValidationException;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;

/**
 * Service wrapper for an {@link CredentialsDefinitionRepository} which enforces permissions and
 * other validations.
 *
 * @see com.netflix.spinnaker.credentials.constraint.ValidatedCredentials
 */
@Log4j2
@RequiredArgsConstructor
@Service
@Validated
public class CredentialsDefinitionService {
  private final CredentialsDefinitionRepository repository;
  private final PermissionEvaluator permissionEvaluator;
  private final ObjectProvider<CredentialsRepository<? extends Credentials>>
      credentialsRepositories;

  private boolean isNameInUse(String name) {
    return repository.findByName(name) != null
        || credentialsRepositories.stream().anyMatch(repo -> repo.has(name));
  }

  private boolean shouldDenyWritePermission(Authentication auth, String name) {
    CredentialsDefinition storedCredentials = repository.findByName(name);
    if (storedCredentials instanceof AccessControlled) {
      return !permissionEvaluator.hasPermission(auth, storedCredentials, Authorization.WRITE);
    }
    return credentialsRepositories
        .orderedStream()
        .filter(repo -> repo.has(name))
        .map(repo -> repo.getOne(name))
        .filter(Objects::nonNull)
        .filter(AccessControlled.class::isInstance)
        .map(AccessControlled.class::cast)
        .findFirst()
        .map(existing -> !permissionEvaluator.hasPermission(auth, existing, Authorization.WRITE))
        .orElse(false);
  }

  /**
   * Validates the given credential definition for the provided user for the given command.
   *
   * @param mutation credential mutation to validate
   * @param auth user who is validating the account definition
   * @throws ValidationException if validation finds any errors
   * @see CredentialsDefinitionValidator
   */
  public void validate(@Valid ValidateCredentialsMutation mutation, Authentication auth) {
    Errors errors;
    if (mutation instanceof ValidateCredentialsMutation.Create) {
      var credentials = ((ValidateCredentialsMutation.Create) mutation).getCredentials();
      errors = new BeanPropertyBindingResult(credentials, "credentials");
      var name = credentials.getName();
      if (StringUtils.hasText(name)) {
        validateCreate(errors, name);
      }
    } else if (mutation instanceof ValidateCredentialsMutation.Update) {
      var credentials = ((ValidateCredentialsMutation.Update) mutation).getCredentials();
      errors = new BeanPropertyBindingResult(credentials, "credentials");
      var name = credentials.getName();
      if (StringUtils.hasText(name)) {
        validateUpdate(errors, name, auth);
      }
    } else if (mutation instanceof ValidateCredentialsMutation.Save) {
      var credentials = ((ValidateCredentialsMutation.Save) mutation).getCredentials();
      errors = new BeanPropertyBindingResult(credentials, "credentials");
      for (int i = 0; i < credentials.size(); i++) {
        errors.pushNestedPath(String.format("[%d]", i));
        var name = credentials.get(i).getName();
        validateSave(errors, name, auth);
        errors.popNestedPath();
      }
    } else if (mutation instanceof ValidateCredentialsMutation.Delete) {
      var credentials = ((ValidateCredentialsMutation.Delete) mutation).getCredentials();
      errors = new BeanPropertyBindingResult(credentials, "credentials");
      Set<String> unknownNames = repository.getUnknownNames(credentials);
      for (int i = 0; i < credentials.size(); i++) {
        errors.pushNestedPath(String.format("[%d]", i));
        var name = credentials.get(i);
        if (unknownNames.contains(name)) {
          errors.reject(
              CredentialsDefinitionErrorCode.NOT_FOUND.getErrorCode(), "No credentials found");
        } else if (shouldDenyWritePermission(auth, name)) {
          errors.reject(
              CredentialsDefinitionErrorCode.UNAUTHORIZED.getErrorCode(),
              "Missing write permissions for credentials");
        }
        errors.popNestedPath();
      }
    } else {
      throw new InvalidRequestException(
          "Unrecognized validate credentials mutation class: " + mutation.getClass());
    }
    if (errors.hasErrors()) {
      throw new ValidationException(errors.getAllErrors());
    }
  }

  public CredentialsView.Metadata create(
      @Valid CredentialsDefinition definition, Authentication auth) {
    validate(new ValidateCredentialsMutation.Create(definition), auth);
    return repository.create(definition);
  }

  private void validateCreate(Errors errors, String name) {
    if (isNameInUse(name)) {
      errors.rejectValue(
          "name",
          CredentialsDefinitionErrorCode.DUPLICATE_NAME.getErrorCode(),
          "Cannot create a new account with the same name as an existing one");
    }
  }

  public CredentialsView.Metadata update(
      @Valid CredentialsDefinition definition,
      Authentication auth,
      @Nullable List<String> ifMatches) {
    validate(new ValidateCredentialsMutation.Update(definition), auth);
    return CollectionUtils.isEmpty(ifMatches) || ifMatches.get(0).equals("*")
        ? repository.update(definition)
        : repository.updateIfMatch(definition, ifMatches);
  }

  private void validateUpdate(Errors errors, String name, Authentication auth) {
    if (!isNameInUse(name)) {
      errors.rejectValue(
          "name",
          CredentialsDefinitionErrorCode.NOT_FOUND.getErrorCode(),
          "Cannot update an account which does not exist");
    } else if (shouldDenyWritePermission(auth, name)) {
      errors.reject(
          CredentialsDefinitionErrorCode.UNAUTHORIZED.getErrorCode(),
          "Unauthorized to overwrite existing account");
    }
  }

  public CredentialsView.Metadata save(
      @Valid CredentialsDefinition definition, Authentication auth) {
    validate(new ValidateCredentialsMutation.Save(List.of(definition)), auth);
    return repository.save(definition);
  }

  public List<CredentialsView> saveAll(
      List<@Valid CredentialsDefinition> definitions, Authentication auth) {
    validate(new ValidateCredentialsMutation.Save(definitions), auth);
    return repository.saveAll(definitions);
  }

  private void validateSave(Errors errors, String name, Authentication auth) {
    if (isNameInUse(name) && shouldDenyWritePermission(auth, name)) {
      errors.reject(
          CredentialsDefinitionErrorCode.UNAUTHORIZED.getErrorCode(),
          "Unauthorized to overwrite existing account");
    }
  }

  public void delete(@NotBlank String accountName, Authentication auth) {
    if (shouldDenyWritePermission(auth, accountName)) {
      throw new AccessDeniedException(
          String.format("Unauthorized to delete account '%s'", accountName));
    }
    repository.delete(accountName);
  }

  public void deleteAll(Collection<@NotBlank String> accountNames, Authentication auth) {
    Set<String> unauthorizedAccountNamesToDelete =
        accountNames.stream()
            .filter(name -> shouldDenyWritePermission(auth, name))
            .collect(Collectors.toSet());
    if (!unauthorizedAccountNamesToDelete.isEmpty()) {
      throw new AccessDeniedException(
          "Unauthorized to delete account(s): " + unauthorizedAccountNamesToDelete);
    }
    repository.deleteAll(accountNames);
  }

  public List<CredentialsDefinitionRepository.Revision> revisionHistory(
      @NotBlank String accountName) {
    return repository.revisionHistory(accountName);
  }
}
