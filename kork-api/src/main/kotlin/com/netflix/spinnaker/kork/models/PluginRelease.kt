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
package com.netflix.spinnaker.kork.models

import com.netflix.spinnaker.kork.models.validation.constraints.SemVer
import com.netflix.spinnaker.kork.proto.plugins.PluginRelease
import java.time.Instant
import javax.validation.constraints.NotEmpty

/**
 * Represents a single [PluginInfo]'s versioned release, including all information that may be necessary
 * for downloading the plugin.
 */
interface PluginRelease : ProtoModel<PluginRelease> {
  /**
   * The SemVer-compatible version for the release.
   */
  @get:SemVer
  val version: String

  /**
   * The date time the plugin was released.
   */
  val date: Instant

  /**
   * A comma-delimited list of SemVer constraint expressions.
   */
  @get:NotEmpty
  val requires: String

  /**
   * The URL where the plugin release binary can be found.
   */
  @get:NotEmpty
  val url: String

  /**
   * The SHA-512 checksum of the release binary.
   */
  @get:NotEmpty
  val sha512sum: String

  /**
   * Designates whether or not this release is the preferred release to install.
   */
  val preferred: Boolean
}
