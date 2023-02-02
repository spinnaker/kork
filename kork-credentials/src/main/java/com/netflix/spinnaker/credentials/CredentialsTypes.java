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
 */

package com.netflix.spinnaker.credentials;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

/** Helper class for working with credentials types and the {@link CredentialsType} annotation. */
@UtilityClass
public class CredentialsTypes {
  @Nullable
  public static String getCredentialsTypeName(
      @Nonnull Class<? extends CredentialsDefinition> credentialsType) {
    CredentialsType type = credentialsType.getAnnotation(CredentialsType.class);
    if (type != null) {
      return type.value();
    }
    // legacy support
    JsonTypeName annotation = credentialsType.getAnnotation(JsonTypeName.class);
    return annotation != null ? annotation.value() : null;
  }
}
