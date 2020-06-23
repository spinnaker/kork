package com.netflix.spinnaker.kork.plugins.sdk.environment

import com.netflix.spinnaker.kork.plugins.api.environment.Environment
import com.netflix.spinnaker.kork.plugins.sdk.SdkFactory
import org.pf4j.PluginWrapper
import org.springframework.core.env.Environment as SpringEnvironment

class EnvironmentSdkFactory(
  private val springEnvironment: SpringEnvironment
) : SdkFactory {

  override fun create(extensionClass: Class<*>, pluginWrapper: PluginWrapper?): Environment {
    return EnvironmentImpl(springEnvironment)
  }
}
