/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.spinnaker.kork.plugins

import com.netflix.spinnaker.kork.exceptions.IntegrationException
import java.util.regex.Pattern

/**
 * Represents a Plugin's dependency version requirements.
 *
 * Each Plugin Release is able to define a set of version requirements that the Spinnaker environment must satisfy
 * before the Release will be installed and loaded.
 *
 * TODO(rz): Will need to support `&` operator, negation, etc eventually.
 *   Is there a lib out there that supports this already?
 *
 * @param target The target requirement. This can be either a plugin or service.
 * @param operator The version comparison operator.
 * @param version The version constraint.
 */
data class PluginReleaseRequires(
  val target: String,
  val operator: String,
  val version: String
) {

  companion object {
    private val pattern =
      Pattern.compile(
        "^(?<target>[\\w\\-.]+)(?<operator>[><=]{1,2})(?<version>[0-9]+\\.[0-9]+\\.[0-9]+)$")

    fun isValid(requires: String): Boolean =
      requires.split(",").all { pattern.matcher(it).matches() }

    fun validate(requires: String) {
      requires.split(",").forEach {
        if (!isValid(requires)) {
          throw MalformedPluginReleaseRequiresException(requires)
        }
      }
    }

    fun parse(requires: String): List<PluginReleaseRequires> =
      requires.split(",").mapNotNull {
        parseOne(it)
      }

    fun parseOne(requires: String): PluginReleaseRequires? {
      val matcher = pattern.matcher(requires)
      if (matcher.matches()) {
        return PluginReleaseRequires(matcher.group("target"), matcher.group("operator"), matcher.group("version"))
      }
      return null
    }
  }

  internal class MalformedPluginReleaseRequiresException(
    providedRequires: String
  ) : IntegrationException(
    "Requires field '$providedRequires' does not conform to Spinnaker's required format. " +
      "Requires fields must follow a '{target}{operator}{version}[,]' format ($pattern)."
  )
}
