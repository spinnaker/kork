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

package com.netflix.spinnaker.kork.plugins.sdk.environment

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.env.Environment as SpringEnvironment
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class EnvironmentImplTest : JUnit5Minutests {

  fun tests() = rootContext<Fixture> {
    fixture {
      Fixture()
    }

    context("Environment data") {

      test("Gets name from config") {
        every { springEnvironment.getProperty(extensibilityConfigEnvName) } returns "staging"
        expectThat(subject.name).isEqualTo("staging")
      }

      test("Gets name from last active spring profile") {
        every { springEnvironment.activeProfiles } returns arrayOf("spinnaker", "prod", "staging")
        expectThat(subject.name).isEqualTo("staging")
      }

      test("Favors explicitly configured env name") {
        every { springEnvironment.getProperty(extensibilityConfigEnvName) } returns "local"
        every { springEnvironment.activeProfiles } returns arrayOf("spinnaker", "prod", "staging")
        expectThat(subject.name).isEqualTo("local")
      }

      test("Gets region from configured environment variable") {
        every { springEnvironment.getProperty(extensibilityConfigEnvRegionVariable) } returns "REGION"
        every { springEnvironment.getProperty("REGION") } returns "us-west-2"
        expectThat(subject.region).isEqualTo("us-west-2")
      }

      test("Gets region from configured region") {
        every { springEnvironment.getProperty(extensibilityConfigEnvRegion) } returns "us-west-2"
        expectThat(subject.region).isEqualTo("us-west-2")
      }

      test("Favors explicitly configured region") {
        every { springEnvironment.getProperty(extensibilityConfigEnvRegion) } returns "us-east-1"
        every { springEnvironment.getProperty(extensibilityConfigEnvRegionVariable) } returns "REGION"
        every { springEnvironment.getProperty("REGION") } returns "us-west-2"
        expectThat(subject.region).isEqualTo("us-east-1")
      }

      test("Name and region return unknown when nothing configured") {
        every { springEnvironment.getProperty(any()) } returns null
        every { springEnvironment.activeProfiles } returns emptyArray()
        expectThat(subject.name).isEqualTo("default")
        expectThat(subject.region).isEqualTo("default")
      }
    }
  }

  private inner class Fixture {
    val springEnvironment: SpringEnvironment = mockk(relaxed = true)
    val subject = EnvironmentImpl(springEnvironment)

    val extensibilityConfigEnvName = "spinnaker.extensibility.environment.name"
    val extensibilityConfigEnvRegionVariable = "spinnaker.extensibility.environment.regionVariable"
    val extensibilityConfigEnvRegion = "spinnaker.extensibility.environment.region"
  }
}
