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
 *
 */

package com.netflix.spinnaker.credentials;

import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionValidator;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.service.CredentialsDefinitionService;
import com.netflix.spinnaker.credentials.service.CredentialsInspector;
import com.netflix.spinnaker.credentials.service.ValidateCredentialsMutation;
import com.netflix.spinnaker.credentials.types.CredentialsDefinitionTypeMap;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import com.netflix.spinnaker.kork.web.exceptions.ValidationException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Facade for managing credentials storage, inspection, and retrieval. */
@Component
@RequiredArgsConstructor
public class CredentialsManager {
  private final CredentialsDefinitionTypeMap typeMap;
  private final ObjectProvider<CredentialsInspector> inspectorProvider;
  private final ObjectProvider<CredentialsDefinitionService> serviceProvider;

  /**
   * Provides the list of supported type discriminators for credentials. These are dynamically
   * loaded and may be provided by plugins, so this list must be checked at runtime.
   */
  public List<String> listSupportedTypes() {
    return List.copyOf(typeMap.getSupportedTypeNames());
  }

  /**
   * Lists credentials views for the authenticated user. These views are a superset of the
   * credentials a user may use as it will also include potentially damaged credentials whose
   * persisted structure does not match the underlying credentials definition class.
   */
  public List<CredentialsView> listCredentialsViews(Authentication auth) {
    CredentialsInspector inspector = inspectorProvider.getIfAvailable();
    return inspector != null ? inspector.listCredentialsViews(auth) : List.of();
  }

  /**
   * Validates the given credential definition for the provided user for the given command.
   *
   * @param mutation credential mutation to validate
   * @param auth user who is validating the account definition
   * @throws ValidationException if validation finds any errors
   * @see CredentialsDefinitionValidator
   */
  public void validate(ValidateCredentialsMutation mutation, Authentication auth) {
    getService().validate(mutation, auth);
  }

  /**
   * Creates a new credential definition. If credentials already exist for the same name, then this
   * response may include the existing credentials with an HTTP error code, `E-Tag`,
   * `Last-Modified`, and other headers.
   */
  public CredentialsView.Metadata create(CredentialsDefinition definition, Authentication auth) {
    return getService().create(definition, auth);
  }

  /** Saves a credential definition by creating new credentials or updating existing credentials. */
  public CredentialsView.Metadata save(CredentialsDefinition definition, Authentication auth) {
    return getService().save(definition, auth);
  }

  /** Saves a batch of credentials definitions by creating or updating each one as appropriate. */
  public List<CredentialsView> saveAll(
      List<CredentialsDefinition> definitions, Authentication auth) {
    return getService().saveAll(definitions, auth);
  }

  /**
   * Updates existing credentials matching the same name. If no credentials exist with the same
   * name, then this should return an HTTP error code.
   */
  public CredentialsView.Metadata update(
      CredentialsDefinition definition, Authentication auth, List<String> ifMatch) {
    return getService().update(definition, auth, ifMatch);
  }

  /** Deletes a credential definition by name. */
  public void delete(String name, Authentication auth) {
    getService().delete(name, auth);
  }

  /** Delete multiple credentials definitions by name. */
  public void deleteAll(Collection<String> names, Authentication auth) {
    getService().deleteAll(names, auth);
  }

  /** Lists the historical revisions of a credential definition by name. */
  public List<CredentialsDefinitionRepository.Revision> revisionHistory(String name) {
    return getService().revisionHistory(name);
  }

  private CredentialsDefinitionService getService() {
    CredentialsDefinitionService service = serviceProvider.getIfAvailable();
    if (service == null) {
      throw new NotFoundException("Self-service account storage is not enabled");
    }
    return service;
  }
}
