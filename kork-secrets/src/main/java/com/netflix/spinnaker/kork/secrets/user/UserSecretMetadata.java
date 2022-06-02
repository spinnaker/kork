/*
 * Copyright 2022 Apple Inc.
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

package com.netflix.spinnaker.kork.secrets.user;

import com.netflix.spinnaker.kork.annotations.Beta;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@NonnullByDefault
@Getter
@Builder
@Jacksonized
@Beta
public class UserSecretMetadata {
  /** Returns the type of the user secret. */
  private final String type;
  /** Returns the encoding of the user secret. */
  private final String encoding;
  /** Returns the authorized roles that can use the user secret. */
  private final List<String> roles;
}
