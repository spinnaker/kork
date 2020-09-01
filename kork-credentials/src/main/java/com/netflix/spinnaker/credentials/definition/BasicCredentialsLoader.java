/*
 * Copyright 2020 Armory
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

package com.netflix.spinnaker.credentials.definition;

import com.netflix.spinnaker.credentials.Credentials;
import com.netflix.spinnaker.credentials.CredentialsRepository;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

@NonnullByDefault
public class BasicCredentialsLoader<T extends CredentialsDefinition, U extends Credentials>
    extends AbstractCredentialsLoader<U> {
  protected final CredentialsParser<T, U> parser;
  protected final CredentialsDefinitionSource<T> definitionSource;
  protected final Set<String> credentialNames = new HashSet<>();

  public BasicCredentialsLoader(
      CredentialsDefinitionSource<T> definitionSource,
      CredentialsParser<T, U> parser,
      CredentialsRepository<U> credentialsRepository) {
    super(credentialsRepository);
    this.parser = parser;
    this.definitionSource = definitionSource;
  }

  @Override
  public void load() {
    this.parse(definitionSource.getCredentialsDefinitions());
  }

  protected void parse(Collection<T> definitions) {
    Set<String> latestAccountNames =
        definitions.stream().map(T::getName).collect(Collectors.toSet());

    // deleted accounts
    credentialNames.stream()
        .filter(name -> !latestAccountNames.contains(name))
        .peek(credentialsRepository::delete)
        .forEach(credentialNames::remove);

    // modified accounts
    definitions.stream()
        .filter(p -> credentialNames.contains(p.getName()))
        .map(parser::parse)
        .filter(Objects::nonNull)
        .forEach(a -> credentialsRepository.update(a.getName(), a));

    // new accounts
    definitions.stream()
        .filter(p -> !credentialNames.contains(p.getName()))
        .map(parser::parse)
        .filter(Objects::nonNull)
        .peek(a -> credentialsRepository.save(a.getName(), a))
        .forEach(a -> credentialNames.add(a.getName()));
  }
}
