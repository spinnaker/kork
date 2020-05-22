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
package com.netflix.spinnaker.kork.models.impl

import com.netflix.spinnaker.kork.models.PluginInfo
import com.netflix.spinnaker.kork.models.PluginRelease
import com.netflix.spinnaker.kork.models.toProto
import com.netflix.spinnaker.kork.proto.plugins.PluginInfo as PluginInfoProto
import java.util.SortedSet

data class PluginInfoImpl(
  override val id: String,
  override val name: String,
  override val description: String,
  override val provider: String,
  override val projectUrl: String,
  override val releases: SortedSet<PluginRelease> = sortedSetOf()
) : PluginInfo {

  constructor(proto: PluginInfoProto) : this(
    id = proto.id.value,
    name = proto.name.value,
    description = proto.description.value,
    provider = proto.provider.value,
    projectUrl = proto.projectUrl.value,
    releases = proto.releasesList
      .map { PluginReleaseImpl(it) }
      .toSortedSet(Comparator { o1, o2 -> o1.date.compareTo(o2.date) })
  )

  override fun toProto(): PluginInfoProto =
    PluginInfoProto.newBuilder()
      .setId(id.toProto())
      .setName(name.toProto())
      .setDescription(description.toProto())
      .setProvider(provider.toProto())
      .setProjectUrl(projectUrl.toProto())
      .addAllReleases(releases.map { it.toProto() })
      .build()
}

/**
 * Converts a [PluginInfoProto] into a [PluginInfo] model.
 */
fun PluginInfoProto.toModel(): PluginInfo =
  PluginInfoImpl(this)
