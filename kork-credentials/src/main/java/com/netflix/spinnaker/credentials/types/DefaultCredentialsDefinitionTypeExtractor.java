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

package com.netflix.spinnaker.credentials.types;

import com.netflix.spinnaker.credentials.CredentialsTypes;
import com.netflix.spinnaker.credentials.constraint.CredentialsDefinitionTypeExtractor;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * Provides a default implementation for {@link CredentialsDefinitionTypeExtractor} which checks for
 * a {@link com.netflix.spinnaker.credentials.definition.CredentialsType} annotation.
 *
 * @see CredentialsTypes
 */
@Component
public class DefaultCredentialsDefinitionTypeExtractor
    implements CredentialsDefinitionTypeExtractor {
  @Nullable
  @Override
  public String getCredentialsType(@Nonnull CredentialsDefinition definition) {
    return CredentialsTypes.getCredentialsTypeName(definition.getClass());
  }
}
