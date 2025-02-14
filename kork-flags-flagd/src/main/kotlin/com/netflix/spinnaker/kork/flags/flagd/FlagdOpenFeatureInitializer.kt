/*
 * Copyright 2024 Apple, Inc.
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

package com.netflix.spinnaker.kork.flags.flagd

import com.netflix.spinnaker.kork.flags.bindOrCreate
import dev.openfeature.contrib.providers.flagd.Config.Resolver
import dev.openfeature.contrib.providers.flagd.FlagdOptions
import dev.openfeature.contrib.providers.flagd.FlagdProvider
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

class FlagdOpenFeatureInitializer : ApplicationContextInitializer<GenericApplicationContext> {
  override fun initialize(context: GenericApplicationContext) {
    beans {
      val properties = env.bindOrCreate<FlagdProperties>("flags.providers.flagd")
      if (properties.enabled) {
        bean { properties }

        val builder = FlagdOptions.builder().resolverType(Resolver.IN_PROCESS)
        if (properties.sync.enabled) {
          val socketPath = properties.sync.socketPath?.toString()
          if (socketPath != null) {
            builder.socketPath(socketPath)
          } else {
            builder.port(properties.sync.port)
          }
          bean<FlagSyncService>()
          bean<FlagSyncServer>()
        } else if (properties.offline.enabled) {
          val flagSourcePath = requireNotNull(properties.offline.flagSourcePath) {
            "The property 'flags.provider.flagd.offline.enable' is true, but no value defined for 'flags.provider.flagd.offline.flag-source-path'"
          }
          builder.offlineFlagSourcePath(flagSourcePath.toString())
        } else {
          throw InvalidConfigurationPropertyValueException(
            "flags.provider.flagd.enabled",
            true,
            "No flagd source configured"
          )
        }
        bean<FlagdProvider> {
          FlagdProvider(builder.build())
        }
      }
    }.initialize(context)
  }
}
