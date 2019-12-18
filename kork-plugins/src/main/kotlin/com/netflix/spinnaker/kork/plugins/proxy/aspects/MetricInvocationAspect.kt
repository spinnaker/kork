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

package com.netflix.spinnaker.kork.plugins.proxy.aspects

import com.netflix.spectator.api.Id
import com.netflix.spectator.api.Registry
import com.netflix.spectator.api.histogram.PercentileTimer
import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class MetricInvocationAspect(
  private val registry: Registry
) : InvocationAspect<MetricMethodInvocationState> {

  // TODO(jonsie): Add extension name to metricNamespace or to tags??

  override fun supports(methodInvocationState: Class<MethodInvocationState>): Boolean {
    return methodInvocationState == MetricMethodInvocationState::class.java
  }

  override fun create(
    target: Any,
    proxy: Any,
    method: Method,
    args: Array<out Any>?,
    descriptor: SpinnakerPluginDescriptor
  ): MetricMethodInvocationState {
    return MetricMethodInvocationState(
      System.currentTimeMillis(),
      timingId(method, descriptor.pluginId, mapOf(Pair("pluginVersion", descriptor.version))),
      invocationId(method, descriptor.pluginId, mapOf(Pair("pluginVersion", descriptor.version)))
    )
  }

  override fun after(success: Boolean, methodInvocationState: MetricMethodInvocationState) {
    val result = if (success) "success" else "failure"

    registry
      .counter(methodInvocationState.invocationsId.withTag("result", result))
      .increment()
    recordTiming(methodInvocationState.timingId.withTag("result", result), methodInvocationState.startTime)
  }

  private fun recordTiming(id: Id, startTimeMs: Long) {
    PercentileTimer.get(registry, id)
      .record(System.currentTimeMillis() - startTimeMs, TimeUnit.MILLISECONDS)
  }

  private fun invocationId(method: Method, metricNamespace: String, tags: Map<String, String>): Id {
    return registry.createId(toMetricId(method, metricNamespace, "invocations"), tags)
  }

  private fun timingId(method: Method, metricNamespace: String, tags: Map<String, String>): Id {
    return registry.createId(toMetricId(method, metricNamespace, "timing"), tags)
  }

  private fun toMetricId(method: Method, metricNamespace: String, metricName: String): String? {
    val methodName = if (method.parameterCount == 0) method.name else String.format("%s%d", method.name, method.parameterCount)
    return toMetricId(metricNamespace, methodName, metricName)
  }

  private fun toMetricId(metricNamespace: String, methodName: String, metricName: String): String? {
    return String.format("%s.%s.%s", metricNamespace, methodName, metricName)
  }
}
