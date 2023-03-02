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
import java.util.List;
import java.util.Map;
import lombok.Data;

/** Provides a view into a credential definition for a user. */
@Data
public class CredentialsView {
  private final Metadata metadata = new Metadata();

  /**
   * Provides the current specification of the credentials. This may be an instance of a {@link
   * Map}, a {@code JsonNode}, or a {@link CredentialsDefinition} depending on how far the parser
   * gets before encountering an error. For a map result, this will contain a key named {@code data}
   * pointing to a string containing the invalid JSON data. For a {@code JsonNode} result, this is
   * valid JSON but not in the expected form of a {@link CredentialsDefinition}. For a {@link
   * CredentialsDefinition}, this will not have any secrets decrypted.
   */
  private Object spec;

  private final Status status = new Status();

  /**
   * Provides metadata regarding a credential definition including the account name, the source from
   * which it came, and the type of credentials (corresponding to its {@link CredentialsType} type
   * discriminator value).
   *
   * @see CredentialsSource
   */
  @Data
  public static class Metadata {
    /** Provides the name of the account. */
    private String name;
    /** Provides the type discriminator of the credentials. May be null if unknown. */
    private String type;
    /** Describes the source of this credential definition. */
    private CredentialsSource source;
  }

  /** Describes the status of this credential definition. */
  @Data
  public static class Status {
    /**
     * Indicates if these credentials are valid. If false, then the errors list will be non-empty.
     */
    private boolean valid;
    /** Lists the errors encountered if the account is invalid. */
    private List<CredentialsError> errors;
  }
}
