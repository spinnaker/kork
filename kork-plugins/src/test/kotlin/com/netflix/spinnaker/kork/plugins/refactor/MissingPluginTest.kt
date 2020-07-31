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

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.mockk
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MissingPluginTest : JUnit5Minutests {

  fun tests() = rootContext {
    val rule: PluginRefactorRule = mockk()

    test("toMessage formatting without minimum version") {
      val missingPlugin = MissingPlugin("missing.plugin", "Some explanation", "ignoreme", rule)
      expectThat(missingPlugin.toMessage()).isEqualTo(
        "${rule.javaClass.simpleName} has detected plugin 'missing.plugin' is missing: Some explanation. " +
          "If you know this plugin's capabilities are satisfied another way, you may ignore this rule by setting " +
          "'spinnaker.extensibility.refactor-rules.ignore.ignoreme' to 'true'"
      )
    }

    test("toMessage formatting with minimum version") {
      val missingPlugin = MissingPlugin("missing.plugin", "Some explanation", "ignoreme", rule, "0.0.1")
      expectThat(missingPlugin.toMessage()).isEqualTo(
        "${rule.javaClass.simpleName} has detected plugin 'missing.plugin' (>=0.0.1) is missing: Some explanation. " +
          "If you know this plugin's capabilities are satisfied another way, you may ignore this rule by setting " +
          "'spinnaker.extensibility.refactor-rules.ignore.ignoreme' to 'true'"
      )
    }
  }
}
