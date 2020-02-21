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

package com.netflix.spinnaker.kork.plugins.update.downloader

import com.netflix.spinnaker.config.PluginsConfigurationProperties
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class FileDownloaderProviderTest : JUnit5Minutests {

  fun tests() = rootContext<FileDownloaderProvider> {
    context("file download provider") {
      fixture {
        FileDownloaderProvider(CompositeFileDownloader(listOf()))
      }

      test("creates the ProcessFileDownloader with custom config") {
        expectThat(get(
          PluginsConfigurationProperties.PluginRepositoryProperties.FileDownloaderProperties().apply {
            className = ProcessFileDownloader::class.java.canonicalName
            config = ProcessFileDownloaderConfig("curl -O")
          }))
          .isA<ProcessFileDownloader>()
          .get { config.command }.isEqualTo("curl -O")
      }

      test("defaults to CompositeFileDownloader") {
        expectThat(get(null))
          .isA<CompositeFileDownloader>()
      }
    }
  }
}
