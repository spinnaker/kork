/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.spinnaker.kork.plugins.v2.scenarios

import com.netflix.spinnaker.config.PluginsAutoConfiguration
import com.netflix.spinnaker.kork.plugins.FRAMEWORK_V2
import com.netflix.spinnaker.kork.plugins.FRAMEWORK_VERSION_CONFIG
import com.netflix.spinnaker.kork.plugins.api.PluginSdks
import com.netflix.spinnaker.kork.plugins.testplugin.testPlugin
import com.netflix.spinnaker.kork.plugins.v2.ApplicationContextGraph
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class SdksAutowiringTest : JUnit5Minutests {

  fun tests() = rootContext<Fixture> {
    fixture {
      Fixture()
    }

    test("PluginSdks is autowired") {
      app.run { ctx: AssertableApplicationContext ->
        val pluginSdks = ctx.getBean(PluginSdks::class.java)

        expectThat(ApplicationContextGraph.pluginContext(generated.plugin.pluginId))
          .isNotNull()
          .get {
            getBean("myExtension").let {
              val field = it.javaClass.getDeclaredField("pluginSdks")
              field.get(it)
            }
          }.isEqualTo(pluginSdks)
      }
    }
  }

  private inner class Fixture {
    val generated = GENERATED
    val app = ApplicationContextRunner()
      .withPropertyValues(
        "spring.application.name=kork",
        "$FRAMEWORK_VERSION_CONFIG=$FRAMEWORK_V2",
        "spinnaker.extensibility.plugins-root-path=${generated.rootPath.toAbsolutePath()}"
      )
      .withConfiguration(
        AutoConfigurations.of(
          PluginsAutoConfiguration::class.java
        )
      )
      .enablePlugin(generated.plugin.pluginId)
  }

  companion object {
    private val GENERATED = testPlugin {
      sourceFile(
        "MyExtension",
        """
          package {{basePackageName}};

          import com.netflix.spinnaker.kork.plugins.api.PluginSdks;
          import com.netflix.spinnaker.kork.plugins.api.internal.SpinnakerExtensionPoint;

          public class {{simpleName}} implements SpinnakerExtensionPoint {

            public PluginSdks pluginSdks;

            public {{simpleName}}(PluginSdks pluginSdks) {
              this.pluginSdks = pluginSdks;
            }
          }
        """.trimIndent()
      )
    }.run { generate() }
  }
}
