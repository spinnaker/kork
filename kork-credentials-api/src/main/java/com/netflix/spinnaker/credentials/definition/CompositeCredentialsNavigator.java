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
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Composite implementation of {@link CredentialsNavigator} for combining a collection of {@link
 * CredentialsDefinitionSource} beans of the same credential type.
 *
 * @param <T> type of credentials
 */
@Alpha
@RequiredArgsConstructor
public class CompositeCredentialsNavigator<T extends CredentialsDefinition>
    implements CredentialsNavigator<T> {
  private final List<CredentialsDefinitionSource<T>> sources;
  @Getter private final Class<T> type;
  @Getter private final String typeName;

  @Override
  public List<T> getCredentialsDefinitions() {
    return sources.stream()
        .flatMap(source -> source.getCredentialsDefinitions().stream())
        .collect(Collectors.toList());
  }

  @Override
  public List<CredentialsView> listCredentialsViews() {
    return sources.stream()
        .flatMap(
            source -> {
              if (source instanceof CredentialsNavigator<?>) {
                // this source already knows how to list views
                return ((CredentialsNavigator<T>) source).listCredentialsViews().stream();
              }
              // otherwise, create views just like super::listCredentialsViews
              return source.getCredentialsDefinitions().stream()
                  .map(
                      definition -> {
                        var metadata = new CredentialsView.Metadata();
                        metadata.setType(getTypeName());
                        metadata.setName(definition.getName());
                        metadata.setSource(source.getSource());
                        return new CredentialsView(metadata, definition);
                      });
            })
        .collect(Collectors.toList());
  }
}
