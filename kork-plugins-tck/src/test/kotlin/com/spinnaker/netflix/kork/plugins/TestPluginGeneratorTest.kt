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
package com.spinnaker.netflix.kork.plugins

import com.netflix.spinnaker.kork.plugins.testplugin.TestPluginGenerator
import com.netflix.spinnaker.kork.plugins.testplugin.testPlugin
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.java.isDirectory
import strikt.java.isRegularFile
import java.nio.file.Files
import java.nio.file.Path

class TestPluginGeneratorTest : JUnit5Minutests {

  fun tests() = rootContext<Path> {
    fixture {
      GENERATED_PATH
    }

    context("classes directory") {
      test("classes directory exists in root") {
        expectThat(resolve("classes")).describedAs("classes directory").isDirectory()
      }

      test("generated class is written to subdirectories matching package") {
        expectThat(resolve("classes/com/netflix/spinnaker/kork/plugins/testplugin/generated")).and {
          isDirectory()
          get { resolve("Generated.class") }.isRegularFile()
        }
      }
    }

    test("plugin properties is written to root") {
      expectThat(resolve("plugin.properties")).and {
        isRegularFile()
        get { toFile().readText() }.isEqualTo(
          """
            plugin.id=spinnaker.generated-testplugin
            plugin.description=A generated TestPlugin named Generated
            plugin.class=com.netflix.spinnaker.kork.plugins.testplugin.generated.Generated
            plugin.version=0.0.1
            plugin.provider=Spinnaker
            plugin.dependencies=
            plugin.requires=*
            plugin.license=Apache 2.0
            plugin.unsafe=false
          """.trimIndent()
        )
      }
    }
  }

  companion object {
    val GENERATED_PATH: Path = Files.createTempDirectory("TestPluginGeneratorTest").let {
      it.toFile().deleteOnExit()
      TestPluginGenerator(testPlugin {}, it).generate()
      it.toFile().listFiles().first().toPath()
    }
  }
}
