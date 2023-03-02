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

package com.netflix.spinnaker.credentials.definition;

import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.kork.annotations.Alpha;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Navigates the sea of credentials provided by a Spinnaker service.
 *
 * @param <T> type of credentials definition tracked
 */
@Alpha
public interface CredentialsNavigator<T extends CredentialsDefinition>
    extends CredentialsDefinitionSource<T> {

  /** Returns the {@link CredentialsType} type name tracked by this navigator. */
  String getTypeName();

  /** Returns the class of the credentials definition type supported. */
  Class<T> getType();

  /**
   * Lists all views into credentials accessible to the current user of the type tracked by this
   * navigator.
   */
  default List<CredentialsView> listCredentialsViews() {
    return getCredentialsDefinitions().stream()
        .map(
            definition -> {
              CredentialsView view = new CredentialsView();
              view.getMetadata().setSource(getSource());
              view.getMetadata().setType(getTypeName());
              view.getMetadata().setName(definition.getName());
              view.setSpec(definition);
              // valid for now until proven otherwise
              view.getStatus().setValid(true);
              return view;
            })
        .collect(Collectors.toList());
  }

  default Optional<T> findByName(String name) {
    Class<T> type = getType();
    return getCredentialsDefinitions().stream()
        .filter(definition -> type.isInstance(definition) && name.equals(definition.getName()))
        .map(type::cast)
        .findFirst();
  }
}
