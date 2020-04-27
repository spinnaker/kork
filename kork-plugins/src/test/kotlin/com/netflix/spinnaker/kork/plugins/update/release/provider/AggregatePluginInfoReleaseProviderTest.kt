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

package com.netflix.spinnaker.kork.plugins.update.release.provider

import com.netflix.spinnaker.kork.plugins.SpringStrictPluginLoaderStatusProvider
import com.netflix.spinnaker.kork.plugins.update.release.PluginInfoRelease
import com.netflix.spinnaker.kork.plugins.update.release.plugin1
import com.netflix.spinnaker.kork.plugins.update.release.plugin2
import com.netflix.spinnaker.kork.plugins.update.release.pluginNoReleases
import com.netflix.spinnaker.kork.plugins.update.release.source.PluginInfoReleaseSource
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockk
import org.pf4j.update.PluginInfo
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo


class AggregatePluginInfoReleaseProviderTest : JUnit5Minutests {
  fun tests() = rootContext<Fixture> {
    fixture {
      Fixture()
    }

    test("Gets a release for the plugin where plugin version") {

    }


  }

  private class Fixture {
    val pluginInfoList = mutableListOf(plugin1, plugin2, pluginNoReleases)
    val pluginInfoReleaseSources: List<PluginInfoReleaseSource> = mutableListOf(FirstPluginInfoReleaseSource(), LastPluginInfoReleaseSource())
    val pluginLoaderStatusProvider: SpringStrictPluginLoaderStatusProvider = mockk(relaxed = true)
    val subject = AggregatePluginInfoReleaseProvider(pluginInfoReleaseSources, pluginLoaderStatusProvider)
  }

  private class FirstPluginInfoReleaseSource : PluginInfoReleaseSource {
    override fun getReleases(pluginInfo: List<PluginInfo>): Set<PluginInfoRelease> {
      return mutableSetOf(PluginInfoRelease(plugin1.id, plugin1.releases[0]), PluginInfoRelease(plugin2.id, plugin1.releases[0]))
    }

    override fun getOrder(): Int = 0
  }

  private class LastPluginInfoReleaseSource : PluginInfoReleaseSource {
    override fun getReleases(pluginInfo: List<PluginInfo>): Set<PluginInfoRelease> {
      return mutableSetOf(PluginInfoRelease(plugin1.id, plugin1.releases[0]), PluginInfoRelease(plugin2.id, plugin1.releases[0]))
    }

    override fun getOrder(): Int = 1
  }
}
