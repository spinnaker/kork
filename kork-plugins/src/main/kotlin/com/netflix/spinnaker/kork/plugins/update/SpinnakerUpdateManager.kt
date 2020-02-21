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

import com.netflix.spinnaker.kork.plugins.SpinnakerServiceVersionManager
import org.pf4j.PluginManager
import org.pf4j.update.PluginInfo.PluginRelease
import org.pf4j.update.UpdateManager
import org.pf4j.update.UpdateRepository
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * TODO(jonsie): [org.pf4j.update.UpdateManager] is overloaded.  It fetches and refreshes plugins
 *  from update repositories, downloads plugins, loads plugins, starts plugins, and has some logic
 *  to select plugin versions.  We have disabled update, load, and start plugin logic here.
 *  This is now used only to manage the list of [UpdateRepository] objects, download the desired
 *  artifact, and check version constraints via an implementation of [org.pf4j.VersionManager].
 *  At some point, we may want to consider removing [org.pf4j.update.UpdateManager] altogether.
 */
class SpinnakerUpdateManager(
  private val pluginManager: PluginManager,
  repositories: List<UpdateRepository>
) : UpdateManager(pluginManager, repositories) {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  /**
   * This method is not supported as it calls pluginManager.loadPlugin and pluginManager.startPlugin.
   * Instead, we only want to install the plugins (see [PluginDownloadService]) and leave loading
   * and starting to [com.netflix.spinnaker.kork.plugins.ExtensionBeanDefinitionRegistryPostProcessor].
   */
  @Synchronized
  override fun installPlugin(id: String?, version: String?): Boolean {
    throw UnsupportedOperationException("UpdateManager installPlugin is not supported")
  }

  /**
   * This method is not supported as it calls pluginManager.loadPlugin and pluginManager.startPlugin.
   * Instead, we only want to install the plugins (see [PluginUpdateService]) and leave loading
   * and starting to [com.netflix.spinnaker.kork.plugins.ExtensionBeanDefinitionRegistryPostProcessor].
   */
  override fun updatePlugin(id: String?, version: String?): Boolean {
    throw UnsupportedOperationException("UpdateManager updatePlugin is not supported")
  }

  /**
   * Supports the scenario wherein we want the latest plugin for the specified service (i.e., not
   * necessarily the service that executes this code).
   *
   * For example, Gate fetches plugins for Deck - so we need to pass in the required service for
   * Deck.
   */
  fun getLastPluginRelease(id: String, serviceName: String): PluginRelease? {
    val pluginInfo = pluginsMap[id] as SpinnakerPluginInfo
    val systemVersion = pluginManager.systemVersion
    val versionManager = SpinnakerServiceVersionManager(serviceName)
    val lastPluginRelease: MutableMap<String, PluginRelease> = mutableMapOf()

    for (release in pluginInfo.getReleases()) {
      if (systemVersion == "0.0.0" || versionManager.checkVersionConstraint(systemVersion, release.requires)) {
        if (lastPluginRelease[id] == null) {
          lastPluginRelease[id] = release
        } else if (versionManager.compareVersions(release.version, lastPluginRelease[id]!!.version) > 0) {
          lastPluginRelease[id] = release
        }
      }
    }

    return lastPluginRelease[id]
  }

  /**
   * Exists to expose protected [downloadPlugin]
   *
   * TODO(jonsie): This will call [UpdateManager.getLastPluginRelease] if `version`
   *  is null.  Shouldn't happen, but it could.  That is potentially problematic if the desired
   *  service name is different than the service that executes this code.  Probably another reason
   *  to consider moving away from [UpdateManager].
   */
  fun downloadPluginRelease(pluginId: String, version: String): Path {
    return downloadPlugin(pluginId, version)
  }
}
