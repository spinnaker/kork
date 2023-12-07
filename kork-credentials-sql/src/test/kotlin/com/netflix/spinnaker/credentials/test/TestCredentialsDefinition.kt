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

package com.netflix.spinnaker.credentials.test

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition
import com.netflix.spinnaker.credentials.definition.CredentialsType
import com.netflix.spinnaker.security.Authorization
import com.netflix.spinnaker.security.AuthorizationMapControlled

@CredentialsType("test")
data class TestCredentialsDefinition(
  private var name: String,
  var attributes: Map<String, String>,
  private var permissions: Map<Authorization, Set<String>>,
) : CredentialsDefinition, AuthorizationMapControlled {
  override fun getName(): String = name

  override fun getPermissions(): Map<Authorization, Set<String>> = permissions
}
