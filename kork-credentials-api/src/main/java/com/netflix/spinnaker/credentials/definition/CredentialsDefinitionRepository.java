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
import com.netflix.spinnaker.credentials.definition.error.DuplicateCredentialsDefinitionException;
import com.netflix.spinnaker.credentials.definition.error.GenericCredentialsDefinitionRepositoryException;
import com.netflix.spinnaker.credentials.definition.error.InvalidCredentialsDefinitionException;
import com.netflix.spinnaker.credentials.definition.error.NoMatchingCredentialsException;
import com.netflix.spinnaker.credentials.definition.error.NoSuchCredentialsDefinitionException;
import com.netflix.spinnaker.kork.annotations.Alpha;
import com.netflix.spinnaker.kork.exceptions.InvalidCredentialsTypeException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;

/** Provides CRUD persistence operations for account {@link CredentialsDefinition} instances. */
@Alpha
public interface CredentialsDefinitionRepository {

  /**
   * Searches for an existing credentials definition with the given name. If the credentials contain
   * secret references, then those will be fetched and decrypted.
   *
   * @param name name of credentials to look up
   * @return the credentials with that name or empty if none exist
   */
  @Nullable
  CredentialsDefinition findByName(String name);

  @Nullable
  CredentialsView.Metadata findMetadataByName(String name);

  /**
   * Lists credentials definitions for a given credentials type. Credential types correspond to the
   * value in the {@link CredentialsType} or {@code com.fasterxml.jackson.annotation.JsonTypeName}
   * annotation present on the corresponding {@link CredentialsDefinition} class. If the credentials
   * contain secret references, then those will be fetched and decrypted.
   *
   * @param typeName credential type to search for
   * @return list of all stored credentials definitions matching the given credentials type name
   */
  List<CredentialsDefinition> listByType(String typeName);

  /**
   * Lists credentials definitions for a given credentials class. This works similarly to {@link
   * #listByType(String)}, though the type name is extracted from the given class, and the result is
   * safely cast to the provided class.
   *
   * @param type credential type to search for
   * @return list of all stored credentials definitions with the given type
   * @param <T> type of credentials
   */
  <T extends CredentialsDefinition> List<T> listByType(Class<T> type);

  /**
   * Lists credentials views for a given credentials type. These views should be post-processed by a
   * {@link CredentialsNavigator}
   *
   * @return list of accessible credentials and their statuses
   */
  List<CredentialsView> listCredentialsViews(String typeName);

  /**
   * Creates a new credentials definition using the provided data. Secrets should use {@code
   * UserSecretReference} encrypted URIs (e.g., {@code
   * secret://secrets-manager?r=us-west-2&s=my-account-credentials}); raw secrets on sensitive
   * fields will be excluded by default. Encrypted URIs will only be decrypted when loading account
   * definitions, not when storing them. Note that account definitions correspond to the JSON
   * representation of the underlying {@link CredentialsDefinition} object along with a JSON type
   * discriminator field with the key {@code type} and value of the corresponding {@link
   * CredentialsType} annotation. If an existing account exists for the same name, then this should
   * return the existing account and metadata in a thrown {@link
   * DuplicateCredentialsDefinitionException}.
   *
   * @param definition credentials definition to store as a new account
   * @return metadata for the created credentials
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while saving the
   *     credentials
   * @throws InvalidCredentialsTypeException if the class of the provided credentials is not
   *     annotated with @CredentialsType
   * @throws InvalidCredentialsDefinitionException if there is an error serializing the provided
   *     credentials
   * @throws DuplicateCredentialsDefinitionException if credentials already exist with the same name
   */
  CredentialsView.Metadata create(CredentialsDefinition definition);

  /**
   * Creates or updates a credentials definition using the provided data. This is also known as an
   * upsert operation. See {@link #create(CredentialsDefinition)} for more details.
   *
   * @param definition account definition to save
   * @return metadata for the saved credentials
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while saving the
   *     credentials
   * @throws InvalidCredentialsTypeException if the class of the provided credentials is not
   *     annotated with @CredentialsType
   * @throws InvalidCredentialsDefinitionException if there is an error serializing the provided
   *     credentials
   */
  CredentialsView.Metadata save(CredentialsDefinition definition);

  /**
   * Creates or updates credentials definitions in bulk.
   *
   * @return list of views of saved account definitions
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while saving the
   *     credentials
   * @throws InvalidCredentialsTypeException if the class of the provided credentials is not
   *     annotated with @CredentialsType
   * @throws InvalidCredentialsDefinitionException if there is an error serializing the provided
   *     credentials
   */
  List<CredentialsView> saveAll(Collection<CredentialsDefinition> definitions);

  /**
   * Updates an existing credentials definition using the provided data. See details in {@link
   * #create(CredentialsDefinition)} for details on the format.
   *
   * @param definition updated account definition to replace an existing account
   * @return metadata of the updated account definition
   * @throws NoSuchCredentialsDefinitionException if no account exists with the same name
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while saving the
   *     account
   * @throws InvalidCredentialsTypeException if the class of the provided credentials is not
   *     annotated with @CredentialsType
   * @throws InvalidCredentialsDefinitionException if there is an error serializing the provided
   *     credentials
   * @see #create(CredentialsDefinition)
   */
  CredentialsView.Metadata update(CredentialsDefinition definition);

  /**
   * Updates an existing credentials definition conditional on its existing etag matching one of the
   * provided etag values.
   *
   * @param definition updated account definition to replace an existing account
   * @param ifMatches list of one or more etag values to match an existing account against
   * @return metadata of the updated account definition
   * @throws NoSuchCredentialsDefinitionException if no account exists with the same name
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while saving the
   *     account
   * @throws InvalidCredentialsTypeException if the class of the provided credentials is not
   *     annotated with @CredentialsType
   * @throws InvalidCredentialsDefinitionException if there is an error serializing the provided
   *     credentials
   * @throws NoMatchingCredentialsException if an account exists with the same name and type but
   *     does not match any of the provided etag values
   * @see #create(CredentialsDefinition)
   */
  CredentialsView.Metadata updateIfMatch(CredentialsDefinition definition, List<String> ifMatches);

  /**
   * Returns the subset of names unknown to this repository from the provided collection of names.
   * If this repository has credentials for all the provided names (or {@code names} is empty), then
   * this returns an empty set.
   *
   * @param names credentials names to check
   * @return subset of unknown credentials names
   */
  Set<String> getUnknownNames(Collection<String> names);

  /**
   * Deletes credentials by name.
   *
   * @param name name of account to delete
   * @throws NoSuchCredentialsDefinitionException if no account exists with the same name
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while deleting the
   *     account
   */
  void delete(String name);

  /**
   * Deletes multiple credentials by name.
   *
   * @param names names of accounts to delete
   * @throws NoSuchCredentialsDefinitionException if one or more accounts requested do not exist
   * @throws GenericCredentialsDefinitionRepositoryException if an error occurs while deleting the
   *     accounts
   */
  void deleteAll(Collection<String> names);

  /**
   * Looks up the revision history of credentials by name. Revisions are sorted by latest version
   * first.
   *
   * @param name account name to look up history for
   * @return history of account updates for the given account name
   */
  List<Revision> revisionHistory(String name);

  /**
   * Provides metadata for a credentials definition revision when making updates to credentials via
   * {@link CredentialsDefinitionRepository} APIs.
   */
  @Getter
  class Revision {
    /** Returns the revision id. Revision ids increase over time and are unique within a service. */
    private final long revision;

    /** Returns the instant when this revision was made. */
    private final Instant timestamp;

    /**
     * Returns the credentials definition used in this revision. Returns {@code null} when this
     * revision corresponds to a deletion.
     */
    private final @Nullable CredentialsDefinition account;

    /**
     * Returns the user who created this revision. May return {@code null} if this revision was made
     * before this data began being tracked.
     */
    private final @Nullable String user;

    /** Constructs a revision entry with a revision id, timestamp, and credentials definition. */
    public Revision(long revision, Instant timestamp, @Nullable CredentialsDefinition account) {
      this.revision = revision;
      this.timestamp = timestamp;
      this.account = account;
      this.user = null;
    }

    /** Constructs a revision entry from a revision id, timestamp, definition, and userid. */
    public Revision(
        long revision,
        Instant timestamp,
        @Nullable CredentialsDefinition account,
        @Nullable String user) {
      this.revision = revision;
      this.timestamp = timestamp;
      this.account = account;
      this.user = user;
    }
  }
}
