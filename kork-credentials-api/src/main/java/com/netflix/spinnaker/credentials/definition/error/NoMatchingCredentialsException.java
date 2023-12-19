/*
 * Copyright 2023 Apple Inc.
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
 *
 */

package com.netflix.spinnaker.credentials.definition.error;

import java.time.Instant;
import javax.annotation.Nullable;
import lombok.Getter;

/** Exception thrown when a conditional update fails to match any requested etag. */
public class NoMatchingCredentialsException extends CredentialsDefinitionRepositoryException {
  @Getter private final Object existing;
  @Getter private final String eTag;
  @Getter private final Instant lastModified;

  public NoMatchingCredentialsException(
      Object existing, String name, String eTag, Instant lastModified) {
    this(existing, name, eTag, lastModified, null);
  }

  public NoMatchingCredentialsException(
      Object existing, String name, String eTag, Instant lastModified, @Nullable Throwable cause) {
    super(name, "No matching credentials", cause);
    this.existing = existing;
    this.eTag = eTag;
    this.lastModified = lastModified;
  }
}
