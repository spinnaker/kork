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

import dev.openfeature.contrib.providers.flagd.Config.Resolver
import dev.openfeature.contrib.providers.flagd.FlagdOptions
import dev.openfeature.contrib.providers.flagd.FlagdProvider
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

/**
 * Handles creation of the [FlagdProvider]. Should be set up after [FlagSyncServer] if in use.
 */
@Component
class FlagdProviderFactory {
  @Bean
  fun flagdProvider(properties: FlagdProperties): FlagdProvider {
    val builder = FlagdOptions.builder().resolverType(Resolver.IN_PROCESS)
    if (properties.offline.enabled) {
      val path = requireNotNull(properties.offline.flagSourcePath) {
        "Offline flagd was enabled but no flag source path defined"
      }
      builder.offlineFlagSourcePath(path.toString())
    } else if (properties.sync.enabled) {
      val socketPath = properties.sync.socketPath?.toString()
      if (socketPath != null) {
        builder.socketPath(socketPath)
      } else {
        builder.port(properties.sync.port)
      }
    } else {
      throw UnsupportedOperationException("The flagd provider was enabled but not configured")
    }
    return FlagdProvider(builder.build())
  }
}
