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

import com.netflix.spectator.api.Clock
import com.netflix.spectator.api.DefaultRegistry
import com.netflix.spectator.api.Id
import com.netflix.spectator.api.Registry
import com.netflix.spectator.api.histogram.PercentileTimer
import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import org.springframework.beans.factory.ObjectProvider
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Adds metric instrumentation to extension method invocations.
 *
 * Two metrics will be recorded for any extension: timing and invocations, with an additional tag
 * for "success", having either the value "success" or "failure".
 *
 */
class MetricInvocationAspect(
  private val registryProvider: ObjectProvider<Registry>
) : InvocationAspect<MetricInvocationState> {

  private val methodMetricIds: ConcurrentHashMap<Method, MetricIds> = ConcurrentHashMap()

  private fun ObjectProvider<Registry>.getOrDefault(): Registry {
    return this.getIfAvailable { DefaultRegistry(Clock.SYSTEM) }
  }

  override fun supports(invocationState: Class<InvocationState>): Boolean {
    return invocationState == MetricInvocationState::class.java
  }

  override fun createState(
    target: Any,
    proxy: Any,
    method: Method,
    args: Array<out Any>?,
    descriptor: SpinnakerPluginDescriptor
  ): MetricInvocationState {
    val registry = registryProvider.getOrDefault()
    val metricIds = methodMetricIds.getOrPut(target, method, descriptor, registry)

    return MetricInvocationState(
      startTimeMs = System.currentTimeMillis(),
      timingId = metricIds?.timingId,
      invocationsId = metricIds?.invocationId
    )
  }

  override fun after(success: Boolean, invocationState: MetricInvocationState) {
    val registry = registryProvider.getOrDefault()
    if (invocationState.invocationsId != null && invocationState.timingId != null) {
      val result = if (success) Result.SUCCESS else Result.FAILURE

      registry.counter(invocationState.invocationsId.withTag("result", result.status)).increment()
      PercentileTimer.get(registry, invocationState.timingId.withTag("result", result.status))
        .record(System.currentTimeMillis() - invocationState.startTimeMs, TimeUnit.MILLISECONDS)
    }
  }

  private fun ConcurrentHashMap<Method, MetricIds>.getOrPut(
    target: Any,
    method: Method,
    descriptor: SpinnakerPluginDescriptor,
    registry: Registry
  ): MetricIds? {
    if (!this.containsKey(method)) {
      if (!method.isAllowed()) {
        return null
      }

      val metricIds = MetricIds(
        timingId = registry.createId(toMetricId(method, descriptor.pluginId, TIMING), mapOf(
          Pair("pluginVersion", descriptor.version),
          Pair("pluginExtension", target.javaClass.simpleName.toString())
        )),
        invocationId = registry.createId(toMetricId(method, descriptor.pluginId, INVOCATIONS), mapOf(
          Pair("pluginVersion", descriptor.version),
          Pair("pluginExtension", target.javaClass.simpleName.toString())
        ))
      )

      for ((cachedMethod, cachedMetricIds) in this) {
        if (cachedMetricIds.invocationId.name() == metricIds.invocationId.name()) {
          throw MetricNameCollisionException(target, cachedMethod, method)
        }
      }

      this[method] = metricIds
    }

    return this[method]
  }

  private fun toMetricId(method: Method, metricNamespace: String, metricName: String): String? {
    val methodName = if (method.parameterCount == 0) method.name else String.format("%s%d", method.name, method.parameterCount)
    return toMetricId(metricNamespace, methodName, metricName)
  }

  private fun toMetricId(metricNamespace: String, methodName: String, metricName: String): String? {
    return String.format("%s.%s.%s", metricNamespace, methodName, metricName)
  }

  private class MetricNameCollisionException(target: Any, method1: Method, method2: Method) :
    IllegalStateException(String.format(
    "Metric name collision detected between methods '%s' and '%s' in '%s'",
    method1.toGenericString(),
    method2.toGenericString(),
    target.javaClass.simpleName))

  private data class MetricIds(val timingId: Id, val invocationId: Id)

  companion object {
    private const val INVOCATIONS = "invocations"
    private const val TIMING = "timing"

    enum class Result(val status: String) {
      SUCCESS("success"),
      FAILURE("failure")
    }
  }
}
