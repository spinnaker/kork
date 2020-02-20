/*
 * Copyright 2019 Netflix, Inc.
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
package com.netflix.spinnaker.kork.plugins

import com.netflix.spinnaker.kork.exceptions.IntegrationException
import com.netflix.spinnaker.kork.plugins.api.ExtensionConfiguration
import com.netflix.spinnaker.kork.plugins.config.ConfigFactory
import com.netflix.spinnaker.kork.plugins.config.ConfigResolver
import com.netflix.spinnaker.kork.plugins.sdk.SdkFactory
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockk
import org.pf4j.ExtensionPoint
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class SpinnakerExtensionFactoryTest : JUnit5Minutests {

  fun tests() = rootContext<Fixture> {
    fixture {
      Fixture()
    }

    context("system extensions") {
      test("without config") {
        every { pluginManager.whichPlugin(any()) } returns null
        expectThat(subject.create(NoConfigSystemExtension::class.java))
          .isA<NoConfigSystemExtension>()
      }

      test("with config") {
        every { pluginManager.whichPlugin(any()) } returns null

        val config = ConfiguredSystemExtension.TheConfig()
        every { configResolver.resolve(any(), any<Class<ConfiguredSystemExtension.TheConfig>>()) } returns config

        expectThat(subject.create(ConfiguredSystemExtension::class.java))
          .isA<ConfiguredSystemExtension>().get { config }
          .isEqualTo(config)
      }
    }

    context("plugin extensions") {
      test("without config") {
        every { pluginManager.whichPlugin(any()) } returns pluginWrapper
        every { pluginWrapper.descriptor } returns createPluginDescriptor("pluginz.bestone")

        expectThat(subject.create(MyPlugin.NoConfigExtension::class.java))
          .isA<MyPlugin.NoConfigExtension>()
      }

      test("with config") {
        every { pluginManager.whichPlugin(any()) } returns pluginWrapper
        every { pluginWrapper.descriptor } returns createPluginDescriptor("pluginz.bestone")

        val config = MyPlugin.ConfiguredExtension.TheConfig()
        every { configResolver.resolve(any(), any<Class<MyPlugin.ConfiguredExtension.TheConfig>>()) } returns config

        expectThat(subject.create(MyPlugin.ConfiguredExtension::class.java))
          .isA<MyPlugin.ConfiguredExtension>()
          .get { config }.isEqualTo(config)
      }

      test("with unsupported constructor argument") {
        every { pluginManager.whichPlugin(any()) } returns pluginWrapper
        every { pluginWrapper.descriptor } returns createPluginDescriptor("pluginz.bestone")

        expectThrows<IntegrationException> {
          subject.create(MyPlugin.UnsupportedArgumentExtension::class.java)
        }
      }

      test("with multiple constructors") {
        every { pluginManager.whichPlugin(any()) } returns pluginWrapper
        every { pluginWrapper.descriptor } returns createPluginDescriptor("pluginz.bestone")

        expectThrows<IntegrationException> {
          subject.create(MyPlugin.MultipleConstructorsExtension::class.java)
        }
      }
    }
  }

  private inner class Fixture {
    val configResolver: ConfigResolver = mockk(relaxed = true)
    val pluginManager: SpinnakerPluginManager = mockk(relaxed = true)
    val configFactory: ConfigFactory = ConfigFactory(configResolver)
    val sdkFactories: List<SdkFactory> = listOf()

    val subject = SpinnakerExtensionFactory(pluginManager, configFactory, sdkFactories)
    val pluginWrapper: PluginWrapper = mockk(relaxed = true)
  }

  private fun createPluginDescriptor(pluginId: String): SpinnakerPluginDescriptor {
    val descriptor: SpinnakerPluginDescriptor = mockk(relaxed = true)
    every { descriptor.pluginId } returns pluginId
    return descriptor
  }

  interface TheExtensionPoint : ExtensionPoint

  class NoConfigSystemExtension : TheExtensionPoint

  class ConfiguredSystemExtension(
    private val config: TheConfig
  ) : TheExtensionPoint {

    @ExtensionConfiguration("extension-point-configuration")
    class TheConfig
  }

  class MyPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {

    class NoConfigExtension : TheExtensionPoint

    class ConfiguredExtension(
      private val config: TheConfig
    ) : TheExtensionPoint {

      @ExtensionConfiguration("extension-point-configuration")
      class TheConfig
    }

    class UnsupportedArgumentExtension(
      private val bad: String
    ) : TheExtensionPoint

    class MultipleConstructorsExtension(
      private val validConfig: ValidConfig
    ) : TheExtensionPoint {

      constructor(bad: String, validConfig: ValidConfig) : this(validConfig)

      @ExtensionConfiguration("valid-config")
      class ValidConfig
    }
  }
}
