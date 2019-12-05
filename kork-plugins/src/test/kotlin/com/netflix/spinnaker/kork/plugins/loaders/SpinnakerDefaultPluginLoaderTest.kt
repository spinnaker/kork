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

package com.netflix.spinnaker.kork.plugins.loaders

import com.netflix.spinnaker.kork.plugins.finders.SpinnakerPropertiesPluginDescriptorFinder
import com.netflix.spinnaker.kork.plugins.testplugin.TestPluginBuilder
import com.netflix.spinnaker.kork.plugins.testplugin.api.TestExtension
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.mockk
import org.pf4j.PluginClassLoader
import org.pf4j.PluginDescriptor
import org.pf4j.PluginManager
import strikt.api.expectThat
import strikt.assertions.isTrue
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SpinnakerDefaultPluginLoaderTest : JUnit5Minutests {

  fun tests() = rootContext<Fixture> {
    fixture { Fixture() }

    test("plugin directory is applicable") {
      expectThat(subject.isApplicable(unsafePluginPath)).isTrue()
    }

    test("loads the plugin class") {
      val classpath = subject.loadPlugin(unsafePluginPath, unsafePluginDescriptor)
      expectThat(classpath).isA<UnsafePluginClassLoader>()
      expectThat(classpath.loadClass(unsafePluginDescriptor.pluginClass).name).isEqualTo(unsafePluginDescriptor.pluginClass)
    }

    test("generated plugin directory is applicable") {
      expectThat(subject.isApplicable(generatedPluginPath)).isTrue()
    }

    test("loads the generated plugin") {
      val classpath = subject.loadPlugin(generatedPluginPath, generatedPluginDescriptor)
      expectThat(classpath).isA<PluginClassLoader>()
      val pluginClass = classpath.loadClass(generatedPluginDescriptor.pluginClass)
      expectThat(pluginClass.name).isEqualTo(generatedPluginDescriptor.pluginClass)
      val extensionClassName = "${pluginClass.`package`.name}.${generatedPluginName}TestExtension"
      val extensionClass = classpath.loadClass(extensionClassName)
      expectThat(TestExtension::class.java.isAssignableFrom(extensionClass)).isTrue()
      val extension = extensionClass.newInstance() as TestExtension
      expectThat(extension.testValue).isEqualTo(extensionClass.simpleName)
    }
  }

  private inner class Fixture {
    val unsafePluginPath: Path = Paths.get(javaClass.getResource("/unsafe-testplugin/plugin.properties").toURI()).parent
    val unsafePluginDescriptor: PluginDescriptor = SpinnakerPropertiesPluginDescriptorFinder().find(unsafePluginPath)
    val pluginManager: PluginManager = mockk(relaxed = true)
    val subject = SpinnakerDefaultPluginLoader(pluginManager)
  }

  companion object {
    val generatedPluginName = "SpinnakerDefaultPluginLoaderTest"
    val generatedPluginPath: Path = Files.createTempDirectory("generatedplugin").also {
      TestPluginBuilder(pluginPath = it, name = generatedPluginName).build()
    }
    val generatedPluginDescriptor: PluginDescriptor = SpinnakerPropertiesPluginDescriptorFinder().find(generatedPluginPath)
  }
}
