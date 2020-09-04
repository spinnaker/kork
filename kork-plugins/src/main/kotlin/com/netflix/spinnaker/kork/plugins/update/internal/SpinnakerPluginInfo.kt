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
 */

package com.netflix.spinnaker.kork.plugins.update.internal

import com.fasterxml.jackson.annotation.JsonSetter
import com.netflix.spinnaker.kork.api.plugins.remote.RemoteExtensionConfig
import org.pf4j.update.PluginInfo

/**
 * Spinnaker-specific [PluginInfo].
 *
 * Required to expose [SpinnakerPluginRelease] to the rest of the framework.
 */
class SpinnakerPluginInfo : PluginInfo() {

  /**
   * Get all known releases for a plugin.
   */
  @Suppress("UNCHECKED_CAST")
  fun getReleases(): List<SpinnakerPluginRelease> {
    return releases as List<SpinnakerPluginRelease>
  }

  /**
   * Set all known releases for a plugin.
   */
  @JsonSetter("releases")
  fun setReleases(spinnakerReleases: List<SpinnakerPluginRelease>) {
    releases = spinnakerReleases
  }

  /**
   * It is not guaranteed that the [org.pf4j.update.UpdateRepository] implementation returns a
   * SpinnakerPluginInfo object.  Therefore, additional fields defined here must provide a default
   * value.
   *
   * @param preferred Whether or not the plugin release is preferred. Preferred plugin releases will be more
   *  likely to be loaded than other releases.
   * @param remoteExtensions Any remote extension configs attached to the plugin.
   */
  data class SpinnakerPluginRelease(
    val preferred: Boolean = false,
    val remoteExtensions: MutableList<RemoteExtensionConfig> = mutableListOf()
  ) : PluginRelease()
}
