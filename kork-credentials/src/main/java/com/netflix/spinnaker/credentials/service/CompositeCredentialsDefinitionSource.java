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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netflix.spinnaker.credentials.CredentialsSource;
import com.netflix.spinnaker.credentials.CredentialsTypes;
import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Provides a full list of CredentialsDefinition account instances for a given credentials type.
 * Given an {@link CredentialsDefinitionRepository} bean and an optional list of {@link
 * CredentialsDefinitionSource} beans for a given account type {@code T}, this class combines the
 * lists from all the given credentials definition sources. When no {@link
 * CredentialsDefinitionSource} beans are available for a given account type, then a default source
 * should be specified to wrap any existing Spring configuration beans that provide the same.
 *
 * @param <T> account credentials definition type
 */
@NonnullByDefault
@Log4j2
public class CompositeCredentialsDefinitionSource<T extends CredentialsDefinition>
    implements CredentialsDefinitionSource<T> {

  private final CredentialsDefinitionRepository repository;
  private final Class<T> type;
  @Getter private final String typeName;
  private final List<CredentialsDefinitionSource<T>> configSources;

  // used to report duplicate accounts only once, instead of spamming the logs
  private final Set<String> duplicateAccountNames = ConcurrentHashMap.newKeySet();

  /**
   * Constructs composite {@code CredentialsDefinitionSource<T>} using the provided repository,
   * credential type, and additional sources for credentials of the same type.
   *
   * @param repository the backing repository for managing credential definitions at runtime
   * @param type the credential type supported by this source (must be annotated with {@link
   *     JsonTypeName} or {@link com.netflix.spinnaker.credentials.definition.CredentialsType})
   * @param configSources the list of other credential definition sources to list accounts from
   */
  public CompositeCredentialsDefinitionSource(
      CredentialsDefinitionRepository repository,
      Class<T> type,
      List<CredentialsDefinitionSource<T>> configSources) {
    this.repository = repository;
    this.type = type;
    String typeName = CredentialsTypes.getCredentialsTypeName(type);
    if (typeName == null) {
      throw new IllegalArgumentException(
          "No @CredentialsType or @JsonTypeName annotation found on " + type);
    }
    this.typeName = typeName;
    this.configSources = configSources;
  }

  @Override
  public List<T> getCredentialsDefinitions() {
    Set<String> seen = new HashSet<>();
    return Stream.concat(
            repository.listByType(typeName).stream().map(type::cast),
            configSources.stream().flatMap(source -> source.getCredentialsDefinitions().stream()))
        .filter(
            definition -> {
              String name = definition.getName();
              if (seen.add(name)) {
                // haven't seen yet
                return true;
              }
              if (duplicateAccountNames.add(name)) {
                // first time we've seen this dupe
                log.warn("Duplicate account name detected ({}). Skipping this definition.", name);
              }
              return false;
            })
        .collect(Collectors.toList());
  }

  /**
   * Lists all views into credentials accessible to the current user of the type tracked by this
   * instance.
   */
  public List<CredentialsView> listCredentialsViews() {
    List<CredentialsView> views = new ArrayList<>(repository.listCredentialsViews(typeName));
    for (CredentialsDefinitionSource<T> source : configSources) {
      for (T definition : source.getCredentialsDefinitions()) {
        CredentialsView view = new CredentialsView();
        view.getMetadata().setSource(CredentialsSource.CONFIG);
        view.getMetadata().setType(typeName);
        view.getMetadata().setName(definition.getName());
        view.setSpec(definition);
        // valid for now until proven otherwise
        view.getStatus().setValid(true);
        views.add(view);
      }
    }
    return views;
  }
}
