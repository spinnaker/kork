/*
 * Copyright 2019 Netflix, Inc.
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
package com.netflix.spinnaker.kork.plugins.events

import com.netflix.spinnaker.kork.plugins.update.PluginUpdateService
import org.springframework.context.ApplicationEvent

/**
 * An event emitted whenever a plugin has been downloaded (successfully or not).
 *
 * @param source The [PluginUpdateService], which has emitted this event.
 * @param operation Defines whether the plugin was being installed or updated.
 * @param status Whether or not the plugin download succeeded
 * @param pluginId The ID of the plugin
 * @param version The version of the plugin downloaded
 */
class PluginDownloaded(
  source: PluginUpdateService,
  val operation: Operation,
  val status: Status,
  val pluginId: String,
  val version: String
) : ApplicationEvent(source) {
  enum class Status {
    SUCCEEDED,
    FAILED
  }

  enum class Operation {
    /**
     * When a plugin does not exist yet for a service.
     */
    INSTALL,

    /**
     * When a plugin exists for a service, but is not the correct version.
     */
    UPDATE
  }
}
