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

package com.netflix.spinnaker.credentials.service;

import com.netflix.spinnaker.credentials.CredentialsError;
import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service for inspecting credentials accessible by the current user. This checks for various errors
 * that may occur when mis-configuring credentials.
 */
@NonnullByDefault
@Service
public class CredentialsInspector {
  private final Map<String, CompositeCredentialsDefinitionSource<? extends CredentialsDefinition>>
      sources;
  private final PermissionEvaluator permissionEvaluator;

  public CredentialsInspector(
      ObjectProvider<CredentialsDefinitionSource<? extends CredentialsDefinition>> sources,
      PermissionEvaluator permissionEvaluator) {
    this.sources =
        sources.stream()
            .filter(CompositeCredentialsDefinitionSource.class::isInstance)
            .map(
                source ->
                    (CompositeCredentialsDefinitionSource<? extends CredentialsDefinition>) source)
            .collect(
                Collectors.toMap(
                    CompositeCredentialsDefinitionSource::getTypeName, Function.identity()));
    this.permissionEvaluator = permissionEvaluator;
  }

  /**
   * Enumerates and inspects all credential definitions the given authenticated user has access to.
   * This combines all {@link CredentialsDefinitionRepository} and {@link
   * CredentialsDefinitionSource} definitions. The resulting list of credentials should be checked
   * for duplicate account names. Credentials with permissions defined should be filtered in or out
   * of the resulting list depending on whether the user has {@link Authorization#READ} permission.
   *
   * @param auth the authenticated user to inspect credentials for
   * @return a list of views into the inspected credentials
   */
  public List<CredentialsView> listCredentialsViews(Authentication auth) {
    Set<String> validNames = new HashSet<>();
    return sources.values().stream()
        .flatMap(source -> source.listCredentialsViews().stream())
        .map(
            view -> {
              if (view.getStatus().isValid()) {
                Object spec = view.getSpec();
                // filter out accounts the user shouldn't be able to access
                if (spec instanceof AccessControlled
                    && !permissionEvaluator.hasPermission(auth, spec, Authorization.WRITE)) {
                  return null;
                }

                if (!validNames.add(view.getMetadata().getName())) {
                  view.getStatus().setValid(false);
                  view.getStatus()
                      .setErrors(
                          List.of(
                              new CredentialsError(
                                  CredentialsDefinitionErrorCode.DUPLICATE_NAME.getErrorCode(),
                                  "Duplicate account name",
                                  "name")));
                }
              }
              return view;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
