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

import com.netflix.spinnaker.kork.exceptions.IntegrationException
import com.netflix.spinnaker.kork.plugins.api.spring.SpringPlugin
import com.netflix.spinnaker.kork.plugins.events.ExtensionLoaded
import com.netflix.spinnaker.kork.plugins.update.PluginUpdateService
import org.pf4j.PluginWrapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.AnnotationConfigRegistry
import org.springframework.core.env.StandardEnvironment

/**
 * The primary point of integration between PF4J and Spring, this class is invoked early
 * in the Spring application lifecycle (before any other Beans are created), but after
 * the environment is prepared.
 */
class ExtensionBeanDefinitionRegistryPostProcessor(
  private val pluginManager: SpinnakerPluginManager,
  private val updateManagerService: PluginUpdateService,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val ctx: ApplicationContext?
) : BeanDefinitionRegistryPostProcessor {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
    val start = System.currentTimeMillis()
    log.debug("Preparing plugins")
    pluginManager.loadPlugins()
    updateManagerService.checkForUpdates()
    pluginManager.startPlugins()

    log.debug("Finished preparing plugins in {}ms", System.currentTimeMillis() - start)
  }

  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    log.debug("Creating system extensions")
    pluginManager.getExtensionClassNames(null).forEach {
      log.debug("Creating extension '{}'", it)

      val extensionClass = try {
        javaClass.classLoader.loadClass(it)
      } catch (e: ClassNotFoundException) {
        throw IntegrationException("Could not find system extension class '$it'", e)
      }

      val bean = pluginManager.extensionFactory.create(extensionClass)
      val beanName = "${extensionClass.simpleName.decapitalize()}SystemExtension"

      beanFactory.registerSingleton(beanName, bean)

      applicationEventPublisher.publishEvent(ExtensionLoaded(this, beanName, extensionClass))
    }

    log.debug("Creating plugin extensions")
    pluginManager.startedPlugins.forEach { pluginWrapper ->
      val applicationContext = createSpringContext(pluginWrapper)
      val plugin = pluginWrapper.plugin
      if (plugin is SpringPlugin) {
        plugin.applicationContext = applicationContext
        plugin.initApplicationContext()
      }
      if (!pluginWrapper.isUnsafe()) {
        log.debug("Creating extensions for pluginWrapper '{}'", pluginWrapper.pluginId)
        pluginManager.getExtensionClassNames(pluginWrapper.pluginId).forEach {
          log.debug("Creating extension '{}' for pluginWrapper '{}'", it, pluginWrapper.pluginId)

          val extensionClass = try {
            pluginWrapper.pluginClassLoader.loadClass(it)
          } catch (e: ClassNotFoundException) {
            throw IntegrationException("Could not find extension class '$it' for pluginWrapper '${pluginWrapper.pluginId}'", e)
          }

          val bean = pluginManager.extensionFactory.create(extensionClass)
          val beanName = "${pluginWrapper.pluginId.replace(".", "")}${extensionClass.simpleName.capitalize()}"

          beanFactory.registerSingleton(beanName, bean)

          applicationEventPublisher.publishEvent(
            ExtensionLoaded(this, beanName, extensionClass, pluginWrapper.descriptor as SpinnakerPluginDescriptor)
          )
        }
      }
    }
  }

  private fun createSpringContext(pluginWrapper: PluginWrapper): SpringContextAndRegistryDelegator {
    return if (pluginWrapper.isUnsafe()) {
      SpringContextAndRegistryDelegator(ctx as ConfigurableApplicationContext, ctx as AnnotationConfigRegistry)
    } else {
      SpringContextAndRegistryDelegator(AnnotationConfigApplicationContext().also {
        it.classLoader = pluginWrapper.pluginClassLoader
        it.environment = StandardEnvironment()
      })
    }
  }
}
