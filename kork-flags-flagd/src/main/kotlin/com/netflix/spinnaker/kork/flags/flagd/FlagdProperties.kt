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

import org.springframework.boot.context.properties.bind.DefaultValue
import java.nio.file.Path

data class FlagdProperties(
  @DefaultValue("false") val enabled: Boolean,
  @DefaultValue val sync: Sync,
  @DefaultValue val offline: Offline,
) {
  data class Sync(
    @DefaultValue("false") val enabled: Boolean,
    val socketPath: Path?,
    @DefaultValue("8015") val port: Int,
  )
  data class Offline(
    @DefaultValue("false") val enabled: Boolean,
    val flagSourcePath: Path?,
  )
}
