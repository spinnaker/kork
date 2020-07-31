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

import com.netflix.spinnaker.kork.plugins.SpinnakerPluginManager
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockk
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import strikt.api.expectThrows

class PluginRefactorServiceTest : JUnit5Minutests {

  fun tests() = rootContext<Fixture> {
    context("no rules found") {
      fixture {
        Fixture()
      }
      test("nothing happens") {
        subject.checkForRefactoredCoreFeatures()
      }
    }

    context("rule finds missing plugin") {
      fixture {
        Fixture().apply {
          ruleLocator.rules = listOf(TestPluginRefactorRule())
        }
      }

      test("exception is thrown") {
        expectThrows<MissingRequiredPluginException> {
          subject.checkForRefactoredCoreFeatures()
        }
      }

      test("ignored plugin rules no-op check") {
        every { environment.getProperty(eq(ignoreConfig("ignoreme")), eq(Boolean::class.java), eq(false)) } returns true
        subject.checkForRefactoredCoreFeatures()
      }
    }
  }

  private inner class Fixture {
    val applicationContext: ApplicationContext = mockk(relaxed = true)
    val environment: Environment = mockk(relaxed = true)
    val pluginManager: SpinnakerPluginManager = mockk(relaxed = true)
    val ruleLocator = TestRefactorRuleLocator(listOf())

    val subject = PluginRefactorService(applicationContext, pluginManager).apply {
      refactorRuleLocator = ruleLocator
    }

    init {
      every { applicationContext.environment } returns environment
      every { environment.getProperty(any(), eq(Boolean::class.java), eq(false)) } returns false
    }
  }

  private class TestPluginRefactorRule : PluginRefactorRule {

    override fun checkForMissingPlugins(
      environment: Environment,
      pluginManager: SpinnakerPluginManager
    ): Set<MissingPlugin> =
      setOf(
        MissingPlugin(
          pluginId = "test.missing-plugin",
          message = "Some description",
          ignoreKey = "ignoreme",
          source = this
        )
      )
  }

  private class TestRefactorRuleLocator(
    var rules: List<PluginRefactorRule>
  ) : PluginRefactorService.RefactorRuleLocator {

    override fun locateRules(): List<PluginRefactorRule> = rules
  }
}
