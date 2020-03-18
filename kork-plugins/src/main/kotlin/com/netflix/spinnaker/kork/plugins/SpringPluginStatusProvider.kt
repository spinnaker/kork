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
package com.netflix.spinnaker.kork.plugins

import com.netflix.spinnaker.config.PluginsConfigurationProperties.CONFIG_NAMESPACE
import com.netflix.spinnaker.config.PluginsConfigurationProperties.DEFAULT_ROOT_PATH
import kotlin.collections.set
import org.pf4j.PluginStatusProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource

/**
 * Backs plugin status by the Spring environment, instead of using text files.
 */
class SpringPluginStatusProvider(
  private val environment: Environment
) : PluginStatusProvider, ApplicationListener<ApplicationEnvironmentPreparedEvent> {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  private val propertySourceBackingStore: MutableMap<String, Any> = mutableMapOf()
  private val propertySource = MapPropertySource("plugins", propertySourceBackingStore)

  override fun disablePlugin(pluginId: String) {
    log.info("Disabling plugin: $pluginId")
    propertySourceBackingStore[enabledPropertyName(pluginId)] = false
  }

  override fun isPluginDisabled(pluginId: String): Boolean =
    !isEnabled(pluginId)

  override fun enablePlugin(pluginId: String) {
    log.info("Enabling plugin: $pluginId")
    propertySourceBackingStore[enabledPropertyName(pluginId)] = true
  }

  override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
    log.debug("Adding ${this.javaClass.simpleName} as new PropertySource")
    event.environment.propertySources.addFirst(propertySource)
  }

  private fun isEnabled(pluginId: String): Boolean =
    environment.getProperty(enabledPropertyName(pluginId))?.toBoolean() ?: false

  private fun enabledPropertyName(pluginId: String): String =
    "$ROOT_CONFIG.$pluginId.enabled"

  fun pluginVersion(pluginId: String): String? =
    environment.getProperty(versionPropertyName(pluginId))?.toString()

  private fun versionPropertyName(pluginId: String): String =
    "$ROOT_CONFIG.$pluginId.version"

  companion object {
    const val ROOT_CONFIG: String = "$CONFIG_NAMESPACE.$DEFAULT_ROOT_PATH"
  }
}
