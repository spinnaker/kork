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

package com.netflix.spinnaker.credentials.definition.error;

import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Exception thrown when no credentials definitions exist for a given name or collection of names
 * (bulk operations).
 */
public class NoSuchCredentialsDefinitionException extends CredentialsDefinitionRepositoryException {
  public NoSuchCredentialsDefinitionException(String name, String message) {
    super(name, message);
  }

  public NoSuchCredentialsDefinitionException(
      String name, String message, @Nullable Throwable cause) {
    super(name, message, cause);
  }

  public NoSuchCredentialsDefinitionException(Collection<String> names, String message) {
    super(names, message);
  }
}
