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

import com.netflix.spinnaker.credentials.CredentialsError;
import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsNavigator;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class CredentialsInspector {
  // as this class is instantiated before all credentials-related beans have been defined, this must
  // use lazy loading of beans via ObjectProvider instead of a List or similar collection
  private final ObjectProvider<CredentialsNavigator<? extends CredentialsDefinition>> navigators;
  private final PermissionEvaluator permissionEvaluator;

  /**
   * Enumerates and inspects all credential definitions the given authenticated user has access to.
   * This combines all {@link CredentialsNavigator} beans to iterate over credentials definitions.
   * The resulting list of credentials is checked for duplicate account names. Credentials with
   * permissions defined are filtered in or out of the resulting list depending on whether the user
   * has {@link Authorization#READ} permission.
   *
   * @param auth the authenticated user to inspect credentials for
   * @return a list of views into the inspected credentials
   */
  public List<CredentialsView> listCredentialsViews(Authentication auth) {
    Set<String> validNames = new HashSet<>();
    return navigators.stream()
        .flatMap(navigator -> navigator.listCredentialsViews().stream())
        .map(
            view -> {
              var status = view.getStatus();
              var spec = view.getSpec();
              if (spec instanceof AccessControlled
                  && !permissionEvaluator.hasPermission(auth, spec, Authorization.READ)) {
                // remove unauthorized accounts from results
                return null;
              }
              if (status.isValid() && !validNames.add(view.getMetadata().getName())) {
                status.setValid(false);
                status.setErrors(
                    List.of(
                        new CredentialsError(
                            CredentialsDefinitionErrorCode.DUPLICATE_NAME.getErrorCode(),
                            "Duplicate account name",
                            "name")));
              }
              return view;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
