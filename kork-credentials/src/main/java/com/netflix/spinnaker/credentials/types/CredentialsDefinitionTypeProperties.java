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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionTypeProvider;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import java.util.List;
import lombok.Data;

@Data
public class CredentialsDefinitionTypeProperties {
  /**
   * Additional packages to scan for {@link
   * com.netflix.spinnaker.credentials.definition.CredentialsDefinition} implementation classes that
   * may be annotated with {@link CredentialsType} to participate in the account management system.
   * These packages are in addition to the default scan package from within {@code com.netflix}.
   * Note that this configuration option only works for account types that are compiled in
   * Spinnaker; plugin account types must register a {@link CredentialsDefinitionTypeProvider} bean
   * for additional types.
   */
  private List<String> additionalScanPackages = List.of();
}
