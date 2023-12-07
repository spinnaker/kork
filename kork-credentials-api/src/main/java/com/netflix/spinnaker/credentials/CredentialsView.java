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

package com.netflix.spinnaker.credentials;

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import com.netflix.spinnaker.kork.annotations.Alpha;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provides a view into a credential definition for a user. */
@Data
@Alpha
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsView {
  /**
   * Provides metadata regarding a credential definition including the account name, the source from
   * which it came, and the type of credentials (corresponding to its {@link CredentialsType} type
   * discriminator value).
   *
   * @see CredentialsSource
   */
  private Metadata metadata = new Metadata();

  /**
   * Provides the current specification of the credentials. This may be an instance of a {@link
   * Map}, a {@code JsonNode}, or a {@link CredentialsDefinition} depending on how far the parser
   * gets before encountering an error. For a map result, this will contain a key named {@code data}
   * pointing to a string containing the invalid JSON data. For a {@code JsonNode} result, this is
   * valid JSON but not in the expected form of a {@link CredentialsDefinition}. For a {@link
   * CredentialsDefinition}, this will not have any secrets decrypted. If more than one credential
   * is available from multiple services, this will be a list of them.
   */
  private Object spec;

  /** Describes the status of this credential definition. */
  private final Status status = new Status();

  @Data
  public static class Metadata {
    /** Provides the name of the account. May be null if unknown (and is not likely parseable). */
    @Nullable private String name;
    /** Provides the type discriminator of the credentials. May be null if unknown. */
    @Nullable private String type;
    /** Provides the entity tag of the credentials. If no history is tracked, this may be null. */
    @Nullable private String eTag;
    /**
     * Provides the instant these credentials were last modified. If untracked, this may be null.
     */
    @Nullable private Instant lastModified;

    /** Describes the source of this credential definition. */
    private CredentialsSource source;
  }

  @Data
  public static class Status {
    /**
     * Indicates if these credentials are valid. If false, then the errors list will be non-empty.
     */
    private boolean valid = true;
    /** Lists the errors encountered if the account is invalid. */
    private List<CredentialsError> errors = new ArrayList<>();

    /** Adds an error to this status and resets the {@link #isValid()} flag to {@code false}. */
    public void addError(CredentialsError error) {
      valid = false;
      errors.add(error);
    }

    /**
     * Adds a collection of errors to this status and resets the {@link #isValid()} flag to {@code
     * false}.
     */
    public void addErrors(Collection<CredentialsError> credentialsErrors) {
      valid = false;
      errors.addAll(credentialsErrors);
    }
  }
}
