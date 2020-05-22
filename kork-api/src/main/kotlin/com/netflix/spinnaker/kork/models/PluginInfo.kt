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

import com.netflix.spinnaker.kork.models.validation.constraints.PluginId
import com.netflix.spinnaker.kork.proto.plugins.PluginInfo as PluginInfoProto
import java.util.SortedSet
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

/**
 * Represents a single plugin within Spinnaker.
 */
interface PluginInfo : ProtoModel<PluginInfoProto> {
  /**
   * The plugin ID.
   */
  @get:PluginId
  val id: String

  /**
   * The name of the plugin. This can be whatever meaningful name you'd like
   * to assign it.
   */
  @get:NotEmpty
  val name: String

  /**
   * A description of what the plugin does.
   */
  @get:NotEmpty
  val description: String

  /**
   * Who the provider of the plugin is. This should typically be an email
   * address that users can contact the plugin authors with.
   */
  @get:NotEmpty
  val provider: String

  /**
   * The plugin homepage, or link to the plugin repository.
   */
  @get:NotEmpty
  val projectUrl: String

  /**
   * A set of plugin releases, sorted by version.
   */
  @get:Valid
  val releases: SortedSet<PluginRelease>
}
