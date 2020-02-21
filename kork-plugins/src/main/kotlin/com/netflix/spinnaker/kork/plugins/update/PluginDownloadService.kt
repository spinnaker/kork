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
package com.netflix.spinnaker.kork.plugins.update

import com.netflix.spinnaker.kork.exceptions.IntegrationException
import com.netflix.spinnaker.kork.plugins.SpinnakerPluginManager
import com.netflix.spinnaker.kork.plugins.SpringPluginStatusProvider
import com.netflix.spinnaker.kork.plugins.events.PluginDownloaded
import com.netflix.spinnaker.kork.plugins.events.PluginDownloaded.Status.FAILED
import com.netflix.spinnaker.kork.plugins.events.PluginDownloaded.Status.SUCCEEDED
import org.pf4j.PluginRuntimeException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.io.File
import java.io.IOException
import java.lang.UnsupportedOperationException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The [PluginDownloadService] is responsible for downloading the correct plugins from the plugin
 * update repositories.
 *
 * Plugins will not be loaded or started from this service.  All plugin loading and starting occurs
 * via [com.netflix.spinnaker.kork.plugins.ExtensionBeanDefinitionRegistryPostProcessor].
 *
 */
class PluginDownloadService(
  internal val updateManager: SpinnakerUpdateManager,
  internal val pluginManager: SpinnakerPluginManager,
  private val pluginStatusProvider: SpringPluginStatusProvider,
  private val applicationEventPublisher: ApplicationEventPublisher
) {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  /**
   * This is the algorithm for determining which plugins to download.
   *
   * This function first retrieves all available plugins from the plugin info cache.  It then filters
   * based on plugins that are enabled in the configuration for the service.  After that, it checks
   * the plugin version for the enabled plugin.  If the plugin version does not exist, then the latest
   * plugin release is selected.  Otherwise, it checks if the plugin has a version that matches the
   * configured plugin version AND the constraint for that version satisfies the service version
   * constraint (i.e., orca>=1.0.0 & <2.0.0).
   *
   * TODO(jonsie): Consider removing the fallback and throwing PluginNotFoundException once
   *  plugins are out of beta.
   */
  internal fun downloadPlugins() {
    val availablePlugins = updateManager.availablePlugins as MutableList<SpinnakerPluginInfo>
    log.info("Found '{}' available plugins", availablePlugins.size)

    availablePlugins
      .filter { !pluginStatusProvider.isPluginDisabled(it.id) }
      .forEach plugins@{ plugin ->
        val configuredPluginVersion = pluginStatusProvider.configuredPluginVersion(plugin.id)

        val pluginRelease = if (configuredPluginVersion == null) {
          val fallbackRelease = updateManager.getLastPluginRelease(plugin.id)
            ?: throw PluginNotFoundException(plugin.id, configuredPluginVersion)
          run {
              log.warn("'{}' is enabled but does not have a configured version, falling back to " +
                "version '{}'.", plugin.id, fallbackRelease.version)
              fallbackRelease
          }
        } else {
          plugin.getReleases()
            .filter { release ->
              release.version == configuredPluginVersion
            }
            .firstOrNull { release ->
              pluginManager.versionManager.checkVersionConstraint(release.version, release.requires)
            } ?: throw PluginNotFoundException(plugin.id, configuredPluginVersion)
        }

        log.debug("Downloading plugin '{}' with version '{}'", plugin.id, pluginRelease.version)
        val downloaded = updateManager.downloadPluginRelease(plugin.id, pluginRelease.version)
        val succeeded = pluginManager.pluginsRoot.write(downloaded)

        if (succeeded) {
          log.debug("Downloaded plugin '{}'", plugin.id)
          applicationEventPublisher.publishEvent(
            PluginDownloaded(this, SUCCEEDED, plugin.id, pluginRelease.version)
          )
        } else {
          log.error("Failed downloading plugin '{}'", plugin.id)
          applicationEventPublisher.publishEvent(
            PluginDownloaded(this, FAILED, plugin.id, pluginRelease.version)
          )
        }
      }
  }

  /**
   * Write the plugin, creating the the plugins root directory defined in [pluginManager] if
   * necessary.
   */
  private fun Path.write(downloaded: Path): Boolean {
    if (pluginManager.pluginsRoot == this) {
      val file = this.resolve(downloaded.fileName)
      File(this.toString()).mkdirs()
      try {
        return Files.move(downloaded, file, StandardCopyOption.REPLACE_EXISTING)
          .contains(downloaded.fileName)
      } catch (e: IOException) {
        throw PluginRuntimeException(e, "Failed to write file '{}' to plugins folder", file)
      }
    } else {
      throw UnsupportedOperationException("This operation is only supported on the specified plugins root directory.")
    }
  }

  internal class PluginNotFoundException(pluginId: String, configuredPluginVersion: String?) :
    IntegrationException(
      "'$pluginId' is enabled with version ${configuredPluginVersion ?: "undefined" }, but a " +
        "release version could not be found that satisfies the version and/or the service " +
        "requirement constraints."
  )
}
