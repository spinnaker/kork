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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** Provides CRUD persistence operations for account {@link CredentialsDefinition} instances. */
@Alpha
public interface CredentialsDefinitionRepository {

  /**
   * Searches for an existing credentials definition with the given name.
   *
   * @param name name of account to look for
   * @return the credentials with that name or empty if none exist
   */
  Optional<? extends CredentialsDefinition> findByName(String name);

  /**
   * Lists credentials definitions for a given credentials type. Credential types correspond to the
   * value in the {@link CredentialsType} or {@code com.fasterxml.jackson.annotation.JsonTypeName}
   * annotation present on the corresponding {@link CredentialsDefinition} class.
   *
   * @param typeName credential type to search for
   * @return list of all stored credentials definitions matching the given account type name
   */
  List<? extends CredentialsDefinition> listByType(String typeName);

  /**
   * Lists credentials views for a given credentials type accessible to the current user.
   *
   * @return list of accessible credentials and their statuses
   */
  List<CredentialsView> listCredentialsViews(String typeName);

  /**
   * Creates a new account definition using the provided data. Secrets should use {@code
   * UserSecretReference} encrypted URIs (e.g., {@code
   * secret://secrets-manager?r=us-west-2&s=my-account-credentials}) when the underlying storage
   * provider does not support row-level encryption or equivalent security features. Encrypted URIs
   * will only be decrypted when loading account definitions, not when storing them. Note that
   * account definitions correspond to the JSON representation of the underlying {@link
   * CredentialsDefinition} object along with a JSON type discriminator field with the key {@code
   * type} and value of the corresponding {@link CredentialsType} annotation.
   *
   * @param definition account definition to store as a new account
   */
  void create(CredentialsDefinition definition);

  /**
   * Creates or updates an account definition using the provided data. This is also known as an
   * upsert operation. See {@link #create(CredentialsDefinition)} for more details.
   *
   * @param definition account definition to save
   */
  void save(CredentialsDefinition definition);

  /** Creates or updates account definitions in bulk. */
  void saveAll(Collection<CredentialsDefinition> definitions);

  /**
   * Updates an existing account definition using the provided data. See details in {@link
   * #create(CredentialsDefinition)} for details on the format.
   *
   * @param definition updated account definition to replace an existing account
   * @see #create(CredentialsDefinition)
   */
  void update(CredentialsDefinition definition);

  /**
   * Deletes an account by name.
   *
   * @param name name of account to delete
   */
  void delete(String name);

  /**
   * Deletes multiple accounts by name.
   *
   * @param names names of accounts to delete
   */
  void deleteAll(Collection<String> names);

  /**
   * Looks up the revision history of an account given its name. Revisions are sorted by latest
   * version first.
   *
   * @param name account name to look up history for
   * @return history of account updates for the given account name
   */
  List<Revision> revisionHistory(String name);

  /**
   * Provides metadata for an account definition revision when making updates to an account via
   * {@link CredentialsDefinitionRepository} APIs.
   */
  class Revision {
    private final int version;
    private final long timestamp;
    private final @Nullable CredentialsDefinition account;

    /** Constructs a revision entry with a version and account definition. */
    public Revision(int version, long timestamp, @Nullable CredentialsDefinition account) {
      this.version = version;
      this.timestamp = timestamp;
      this.account = account;
    }

    /** Returns the version number of this revision. Versions start at 1 and increase from there. */
    public int getVersion() {
      return version;
    }

    /**
     * Returns the timestamp (in millis since the epoch) corresponding to when this revision was
     * made.
     */
    public long getTimestamp() {
      return timestamp;
    }

    /**
     * Returns the account definition used in this revision. Returns {@code null} when this revision
     * corresponds to a deletion.
     */
    @Nullable
    public CredentialsDefinition getAccount() {
      return account;
    }
  }
}
