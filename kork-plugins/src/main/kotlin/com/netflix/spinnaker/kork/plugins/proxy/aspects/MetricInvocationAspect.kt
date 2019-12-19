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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MetricInvocationAspect(
  private val registry: Registry
) : InvocationAspect<MetricInvocationState> {

  private val methodMetricIds: ConcurrentHashMap<Method, MetricIds> = ConcurrentHashMap()

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
    val metricIds = methodMetricIds.getOrCache(target, method, descriptor)

    return MetricInvocationState(
      startTimeMs = System.currentTimeMillis(),
      timingId = metricIds?.timingId,
      invocationsId = metricIds?.invocationId
    )
  }

  override fun after(success: Boolean, invocationState: MetricInvocationState) {
    if (invocationState.invocationsId != null && invocationState.timingId != null) {
      val result = if (success) Result.SUCCESS else Result.FAILURE
      registry.counter(invocationState.invocationsId.withTag("result", result.status)).increment()
      recordTiming(invocationState.timingId.withTag("result", result.status), invocationState.startTimeMs)
    }
  }

  private fun recordTiming(id: Id, startTimeMs: Long) {
    PercentileTimer.get(registry, id)
      .record(System.currentTimeMillis() - startTimeMs, TimeUnit.MILLISECONDS)
  }

  private fun invocationId(method: Method, metricNamespace: String, tags: Map<String, String>): Id {
    return registry.createId(toMetricId(method, metricNamespace, INVOCATIONS), tags)
  }

  private fun timingId(method: Method, metricNamespace: String, tags: Map<String, String>): Id {
    return registry.createId(toMetricId(method, metricNamespace, TIMING), tags)
  }

  private fun toMetricId(method: Method, metricNamespace: String, metricName: String): String? {
    val methodName = if (method.parameterCount == 0) method.name else String.format("%s%d", method.name, method.parameterCount)
    return toMetricId(metricNamespace, methodName, metricName)
  }

  private fun toMetricId(metricNamespace: String, methodName: String, metricName: String): String? {
    return String.format("%s.%s.%s", metricNamespace, methodName, metricName)
  }

  private fun ConcurrentHashMap<Method, MetricIds>.getOrCache(
    target: Any,
    method: Method,
    descriptor: SpinnakerPluginDescriptor
  ): MetricIds? {
    if (!this.containsKey(method)) {
      if (!method.isAllowed()) {
        return null
      }

      val metricIds = MetricIds(
        timingId = timingId(method, descriptor.pluginId, mapOf(
          Pair("pluginVersion", descriptor.version),
          Pair("pluginExtension", target.javaClass.simpleName.toString())
        )),
        invocationId = invocationId(method, descriptor.pluginId, mapOf(
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
