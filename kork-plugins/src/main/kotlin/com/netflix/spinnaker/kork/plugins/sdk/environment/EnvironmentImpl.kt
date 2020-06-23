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

import com.netflix.spinnaker.kork.plugins.api.environment.Environment
import org.springframework.core.env.Environment as SpringEnvironment

class EnvironmentImpl(
  private val springEnvironment: SpringEnvironment
) : Environment {

  override fun getName(): String {
    val envFromConfig = springEnvironment.getProperty(extensibilityConfigEnvName)
    val activeProfiles = springEnvironment.activeProfiles

    return if (!envFromConfig.isNullOrEmpty()) {
      envFromConfig
    } else if (!activeProfiles.isNullOrEmpty()) {
      activeProfiles.last()
    } else {
      super.getName()
    }
  }

  override fun getRegion(): String {
    val configuredRegion = springEnvironment.getProperty(extensibilityConfigEnvRegion)
    val regionEnvVariable = springEnvironment.getProperty(extensibilityConfigEnvRegionVariable)

    return if (!configuredRegion.isNullOrEmpty()) {
      configuredRegion
    } else if (!regionEnvVariable.isNullOrEmpty()) {
      springEnvironment.getProperty(regionEnvVariable) ?: super.getRegion()
    } else {
      super.getRegion()
    }
  }

  private companion object {
    const val extensibilityConfigEnvName = "spinnaker.extensibility.environment.name"
    const val extensibilityConfigEnvRegionVariable = "spinnaker.extensibility.environment.regionVariable"
    const val extensibilityConfigEnvRegion = "spinnaker.extensibility.environment.region"
  }
}
