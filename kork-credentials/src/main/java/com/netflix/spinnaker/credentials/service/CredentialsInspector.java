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
import com.netflix.spinnaker.credentials.definition.CredentialsNavigator;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.Authorization;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class CredentialsInspector {
  private final List<CredentialsNavigator<? extends CredentialsDefinition>> navigators;
  private final PermissionEvaluator permissionEvaluator;

  public CredentialsInspector(
      ObjectProvider<CredentialsDefinitionSource<? extends CredentialsDefinition>> sources,
      PermissionEvaluator permissionEvaluator) {
    List<CredentialsNavigator<? extends CredentialsDefinition>> navigators = new ArrayList<>();
    for (CredentialsDefinitionSource<? extends CredentialsDefinition> source : sources) {
      if (source instanceof CredentialsNavigator<?>) {
        CredentialsNavigator<? extends CredentialsDefinition> navigator =
            (CredentialsNavigator<? extends CredentialsDefinition>) source;
        navigators.add(navigator);
      } else {
        log.warn("Unable to navigate credentials source {}", source.getClass());
      }
    }
    this.navigators = Collections.unmodifiableList(navigators);
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
