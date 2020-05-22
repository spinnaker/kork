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

import com.netflix.spinnaker.kork.models.PluginRelease
import com.netflix.spinnaker.kork.models.toInstant
import com.netflix.spinnaker.kork.models.toProto
import com.netflix.spinnaker.kork.models.toTimestamp
import com.netflix.spinnaker.kork.proto.plugins.PluginRelease as PluginReleaseProto
import java.time.Instant

data class PluginReleaseImpl(
  override val version: String,
  override val date: Instant,
  override val requires: String,
  override val url: String,
  override val sha512sum: String,
  override val preferred: Boolean = false
) : PluginRelease {

  constructor(proto: PluginReleaseProto) : this(
    version = proto.version.value,
    date = proto.date.toInstant(),
    requires = proto.requires.value,
    url = proto.url.value,
    sha512sum = proto.sha512Sum.value,
    preferred = proto.preferred
  )

  override fun toProto(): PluginReleaseProto =
    PluginReleaseProto.newBuilder()
      .setVersion(version.toProto())
      .setDate(date.toTimestamp())
      .setRequires(requires.toProto())
      .setUrl(url.toProto())
      .setSha512Sum(sha512sum.toProto())
      .setPreferred(preferred)
      .build()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PluginRelease

    if (version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    return version.hashCode()
  }
}

/**
 * Converts a [PluginReleaseProto] into a [PluginRelease] model.
 */
fun PluginReleaseProto.toModel(): PluginRelease =
  PluginReleaseImpl(this)
