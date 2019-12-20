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

package com.netflix.spinnaker.kork.plugins.proxy.aspects

import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockk
import org.pf4j.PluginDescriptor
import java.lang.reflect.Method

class MetricInvocationAspectTest : JUnit5Minutests {

  fun tests() = rootContext<Fixture> {
    fixture { Fixture() }

    test("creates the MetricInvocationState object with Spectator IDs") {
      val target: Any = mockk(relaxed = true)
      val proxy: Any = mockk(relaxed = true)
      val method: Method = mockk(relaxed = true)
      val args: Array<out Any> = mockk(relaxed = true)
      val spinnakerPluginDescriptor: SpinnakerPluginDescriptor = mockk(relaxed = true) // createPluginDescriptor("netflix.foo", "0.0.1")

      val state = subject.createState(target,
        proxy, method, args, spinnakerPluginDescriptor)
//      expectThat(state).isA<MetricInvocationState>()
    }

    test("This is a test and it does something") {
      assert(true)
    }
  }

  private inner class Fixture {
    val registry: Registry = mockk(relaxed = true)
    val subject = MetricInvocationAspect(registry)
  }

  private fun createMethod(): Method {
    return SomeExtension::class.java.getMethod("helloWorld")
  }

  private fun createPluginDescriptor(pluginId: String, version: String): SpinnakerPluginDescriptor {
    val descriptor: PluginDescriptor = mockk(relaxed = true)
    every { descriptor.pluginId } returns pluginId
    every { descriptor.version } returns version
    return SpinnakerPluginDescriptor(descriptor)
  }

  private class SomeExtension {
    fun helloWorld(): String {
      return "Hello World!"
    }
  }
}
