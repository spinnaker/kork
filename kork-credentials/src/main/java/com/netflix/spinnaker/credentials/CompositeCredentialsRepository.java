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

package com.netflix.spinnaker.credentials;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides access to credentials (or extension of Credentials) across credentials types.
 *
 * @param <T>
 */
public class CompositeCredentialsRepository<T extends Credentials> {
  private Map<String, CredentialsRepository<? extends T>> allRepositories;

  public CompositeCredentialsRepository(List<CredentialsRepository<? extends T>> repositories) {
    allRepositories = new HashMap<>();
    repositories.forEach(this::registerRepository);
  }

  public void registerRepository(CredentialsRepository<? extends T> repository) {
    allRepositories.put(repository.getType(), repository);
  }

  public T getCredentials(String accountName, String type) {
    if (accountName == null || accountName.equals("")) {
      throw new IllegalArgumentException("An account name must be supplied");
    }

    CredentialsRepository<? extends T> repository = allRepositories.get(type);
    if (repository == null) {
      throw new IllegalArgumentException("No credentials of type '" + type + "' found");
    }

    T account = repository.getOne(accountName);
    if (account == null) {
      throw new IllegalArgumentException(
          "Credentials '" + accountName + "' of type '" + type + "' cannot be found");
    }
    return account;
  }

  /**
   * Helper method during migration from single to multiple credential repositories
   *
   * @param accountName
   * @return Account with the given name across all repositories
   */
  public T getFirstCredentialsWithName(String accountName) {
    if (accountName == null || accountName.equals("")) {
      throw new IllegalArgumentException("An account name must be supplied");
    }

    return allRepositories.values().stream()
        .map(r -> r.getOne(accountName))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /** @return All credentials across all repositories */
  public List<T> getAllCredentials() {
    return Collections.unmodifiableList(
        allRepositories.values().stream()
            .map(CredentialsRepository::getAll)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
  }
}
