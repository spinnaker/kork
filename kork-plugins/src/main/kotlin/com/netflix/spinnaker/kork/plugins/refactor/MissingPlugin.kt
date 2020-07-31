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
package com.netflix.spinnaker.kork.plugins.refactor

/**
 * @param pluginId The plugin ID that is missing.
 * @param message An operator-friendly message describing why we think the plugin is missing.
 * @param ignoreKey A configuration key operators can set to permanently ignore a missing plugin.
 * @param source The [PluginRefactorRule] that originated this missing plugin.
 * @param minimumVersion The minimum plugin version that must be used.
 */
data class MissingPlugin(
  val pluginId: String,
  val message: String,
  val ignoreKey: String,
  val source: PluginRefactorRule,
  val minimumVersion: String? = null
) {
  fun toMessage(): String =
    "${source.javaClass.simpleName} has detected plugin '${pluginId}'${minVersionMessage()} is missing: ${message.trim('.')}. " +
      "If you know this plugin's capabilities are satisfied another way, you may ignore this rule by setting " +
      "'${ignoreConfig(ignoreKey)}' to 'true'"

  private fun minVersionMessage(): String =
    if (minimumVersion == null) "" else " (>=$minimumVersion)"
}
