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
package com.netflix.spinnaker.kork.plugins.refactor

import com.netflix.spinnaker.kork.plugins.SpinnakerPluginManager
import com.netflix.spinnaker.kork.plugins.update.SpinnakerUpdateManager
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.Environment
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.util.ClassUtils

/**
 * Provides a standard utility for Spinnaker services to refactor core functionality out into plugins.
 *
 * Services can define a [PluginRefactorRule] that can evaluate the running service environment to determine
 * if a service is configured for functionality that was built into the core service offering, but has since
 * been refactored out into a plugin.
 */
class PluginRefactorService(
  private val applicationContext: ApplicationContext,
  private val pluginManager: SpinnakerPluginManager
) {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  internal var refactorRuleLocator: RefactorRuleLocator = ClassPathScanningRefactorRuleLocator()

  fun checkForRefactoredCoreFeatures() {
    log.info("Checking for missing plugins")

    refactorRuleLocator.locateRules().forEach {
      checkRule(it, applicationContext.environment)
    }
  }

  private fun checkRule(refactorRule: PluginRefactorRule, environment: Environment) {
    refactorRule.checkForMissingPlugins(environment, pluginManager).forEach {
      if (isMissingPluginIgnored(environment, it)) {
        log.info("Ignoring plugin refactor rule '${it.source.javaClass.simpleName}': Disabled via '${it.ignoreKey}'")
        return
      }

      throw MissingRequiredPluginException(it.toMessage())
    }
  }

  private fun isMissingPluginIgnored(environment: Environment, missingPlugin: MissingPlugin): Boolean =
    environment.getProperty(
      ignoreConfig(missingPlugin.ignoreKey),
      Boolean::class.java,
      false
    )

  /**
   * Defines the strategy for locating [PluginRefactorRule] classes.
   */
  internal interface RefactorRuleLocator {
    fun locateRules(): List<PluginRefactorRule>
  }

  /**
   * The [RefactorRuleLocator] used for normal application flows.
   */
  internal class ClassPathScanningRefactorRuleLocator : RefactorRuleLocator {

    override fun locateRules(): List<PluginRefactorRule> {
      // We need to use classpath scanning instead of Spring, since the plugin framework runs so early.
      val provider = ClassPathScanningCandidateComponentProvider(false).apply {
        addIncludeFilter(AssignableTypeFilter(PluginRefactorRule::class.java))
      }

      return provider.findCandidateComponents("com.netflix.spinnaker")
        .mapNotNull { it.beanClassName }
        .map {
          ClassUtils.resolveClassName(it, ClassUtils.getDefaultClassLoader()).newInstance() as PluginRefactorRule
        }
    }
  }
}
