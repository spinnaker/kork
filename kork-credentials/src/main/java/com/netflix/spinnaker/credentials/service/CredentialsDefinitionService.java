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

package com.netflix.spinnaker.credentials.service;

import com.netflix.spinnaker.credentials.Credentials;
import com.netflix.spinnaker.credentials.CredentialsRepository;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionValidator;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.web.exceptions.ValidationException;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;

/**
 * Service wrapper for an {@link CredentialsDefinitionRepository} which enforces permissions and
 * other validations.
 */
@NonnullByDefault
@Log4j2
@RequiredArgsConstructor
@Service
public class CredentialsDefinitionService {
  private final CredentialsDefinitionRepository repository;
  private final PermissionEvaluator permissionEvaluator;
  private final ObjectProvider<CredentialsDefinitionValidator> validators;
  private final ObjectProvider<CredentialsRepository<? extends Credentials>>
      credentialsRepositories;

  private boolean isNameInUse(String name) {
    return repository.findByName(name).isPresent()
        || credentialsRepositories.stream().anyMatch(repo -> repo.has(name));
  }

  private boolean shouldDenyWritePermission(Authentication auth, String name) {
    return repository
        .findByName(name)
        .filter(AccessControlled.class::isInstance)
        .map(AccessControlled.class::cast)
        .or(
            () ->
                credentialsRepositories
                    .orderedStream()
                    .filter(repo -> repo.has(name))
                    .map(repo -> repo.getOne(name))
                    .filter(Objects::nonNull)
                    .filter(AccessControlled.class::isInstance)
                    .map(AccessControlled.class::cast)
                    .findFirst())
        .map(existing -> !permissionEvaluator.hasPermission(auth, existing, Authorization.WRITE))
        .orElse(false);
  }

  /**
   * Validates the given credential definition for the provided user for the given command.
   *
   * @param definition credential definition to validate
   * @param auth user who is validating the account definition
   * @param command the command being validated (create, update, or save)
   * @throws ValidationException if validation finds any errors
   * @throws AccessDeniedException if the user is unauthorized to perform the given command
   * @see CredentialsDefinitionValidator
   */
  public void validate(
      CredentialsDefinition definition, Authentication auth, CredentialsDefinitionCommand command) {
    String name = definition.getName();
    var errors = new BeanPropertyBindingResult(definition, name);
    boolean accountExists = isNameInUse(name);
    if (command == CredentialsDefinitionCommand.UPDATE && !accountExists) {
      errors.rejectValue(
          "name",
          CredentialsDefinitionErrorCode.NOT_FOUND.getErrorCode(),
          "Cannot update an account which does not exist");
    }
    if (accountExists) {
      if (command == CredentialsDefinitionCommand.CREATE) {
        errors.rejectValue(
            "name",
            CredentialsDefinitionErrorCode.DUPLICATE_NAME.getErrorCode(),
            "Cannot create a new account with the same name as an existing one");
      }
      if (shouldDenyWritePermission(auth, name)) {
        throw new AccessDeniedException("Unauthorized to overwrite existing account");
      }
    }
    validators.forEach(validator -> validator.validate(definition, errors, auth));
    if (errors.hasErrors()) {
      throw new ValidationException(errors.getAllErrors());
    }
  }

  public void create(CredentialsDefinition definition, Authentication auth) {
    validate(definition, auth, CredentialsDefinitionCommand.CREATE);
    repository.create(definition);
  }

  public void save(CredentialsDefinition definition, Authentication auth) {
    validate(definition, auth, CredentialsDefinitionCommand.SAVE);
    repository.save(definition);
  }

  public void saveAll(Collection<CredentialsDefinition> definitions, Authentication auth) {
    var errors = new BeanPropertyBindingResult(definitions, "definitions");
    for (CredentialsDefinition definition : definitions) {
      String name = definition.getName();
      errors.pushNestedPath(name);
      if (shouldDenyWritePermission(auth, name)) {
        errors.rejectValue(
            "name",
            CredentialsDefinitionErrorCode.UNAUTHORIZED.getErrorCode(),
            "Unauthorized to overwrite account");
      }
      validators.forEach(validator -> validator.validate(definition, errors, auth));
      errors.popNestedPath();
    }
    if (errors.hasErrors()) {
      throw new ValidationException(errors.getAllErrors());
    }
    repository.saveAll(definitions);
  }

  public void update(CredentialsDefinition definition, Authentication auth) {
    validate(definition, auth, CredentialsDefinitionCommand.UPDATE);
    repository.update(definition);
  }

  public void delete(String accountName, Authentication auth) {
    if (shouldDenyWritePermission(auth, accountName)) {
      throw new AccessDeniedException(
          String.format("Unauthorized to delete account '%s'", accountName));
    }
    repository.delete(accountName);
  }

  public void deleteAll(Collection<String> accountNames, Authentication auth) {
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

  public List<CredentialsDefinitionRepository.Revision> revisionHistory(String accountName) {
    return repository.revisionHistory(accountName);
  }
}
