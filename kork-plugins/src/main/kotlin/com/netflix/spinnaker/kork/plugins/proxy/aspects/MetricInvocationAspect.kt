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

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.netflix.spectator.api.Clock
import com.netflix.spectator.api.DefaultRegistry
import com.netflix.spectator.api.Id
import com.netflix.spectator.api.Registry
import com.netflix.spectator.api.histogram.PercentileTimer
import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit

/**
 * Adds metric instrumentation to extension method invocations.
 *
 * Two metrics will be recorded for any extension: timing and invocations, with an additional tag
 * for "result", having either the value "success" or "failure".
 *
 */
class MetricInvocationAspect(
  private val registryProvider: ObjectProvider<Registry>
) : InvocationAspect<MetricInvocationState> {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  private val methodMetricIds: Cache<Method, MetricIds> = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build<Method, MetricIds>()

  /**
   * Extensions are loaded early in the Spring application lifecycle, and there's a chance that the
   * Spectator [Registry] does not yet exist when an extension method is invoked.  This allows us to
   * fallback to a temporary registry.  Metrics collected in the fallback registry are discarded.
   *
   * Open question - If a fallback registry is returned, can we collect those metrics and then dump
   * them onto the main registry once that exists?
   */
  private fun ObjectProvider<Registry>.getOrFallback(extensionName: String): Registry {
    val registry = this.ifAvailable

    if (registry == null) {
      log.warn("Returning fallback registry for extension={}; metrics collected in fallback are discarded.", extensionName)
      return DefaultRegistry(Clock.SYSTEM)
    }

    return registry
  }

  override fun supports(invocationState: Class<InvocationState>): Boolean {
    return invocationState == MetricInvocationState::class.java
  }

  override fun before(
    target: Any,
    proxy: Any,
    method: Method,
    args: Array<out Any>?,
    descriptor: SpinnakerPluginDescriptor
  ): MetricInvocationState {
    val extensionName = target.javaClass.simpleName.toString()
    val registry = registryProvider.getOrFallback(extensionName)
    val metricIds = methodMetricIds.getOrPut(target, method, descriptor, registry)

    return MetricInvocationState(
      extensionName = extensionName,
      startTimeMs = System.currentTimeMillis(),
      timingId = metricIds?.timingId,
      invocationsId = metricIds?.invocationId
    )
  }

  override fun after(invocationState: MetricInvocationState) {
    recordMetrics(Result.SUCCESS, invocationState)
  }

  override fun error(e: InvocationTargetException, invocationState: MetricInvocationState) {
    recordMetrics(Result.FAILURE, invocationState)
  }

  private fun recordMetrics(result: Result, invocationState: MetricInvocationState) {
    if (invocationState.invocationsId != null && invocationState.timingId != null) {
      val registry = registryProvider.getOrFallback(invocationState.extensionName)

      registry.counter(invocationState.invocationsId.withTag("result", result.toString())).increment()
      PercentileTimer.get(registry, invocationState.timingId.withTag("result", result.toString()))
        .record(System.currentTimeMillis() - invocationState.startTimeMs, TimeUnit.MILLISECONDS)
    }
  }

  /**
   * Skips private methods, otherwise performs a [Cache] `get` which retrieves the cached data or
   * else creates the data and then inserts it into the cache.
   */
  private fun Cache<Method, MetricIds>.getOrPut(
    target: Any,
    method: Method,
    descriptor: SpinnakerPluginDescriptor,
    registry: Registry
  ): MetricIds? {
    if (!Modifier.isPublic(method.modifiers)) {
      return null
    } else {
      return this.get(method) { m ->
        val metricIds = MetricIds(
          timingId = registry.createId(toMetricId(m, descriptor.pluginId, TIMING), mapOf(
            Pair("pluginVersion", descriptor.version),
            Pair("pluginExtension", target.javaClass.simpleName.toString())
          )),
          invocationId = registry.createId(toMetricId(m, descriptor.pluginId, INVOCATIONS), mapOf(
            Pair("pluginVersion", descriptor.version),
            Pair("pluginExtension", target.javaClass.simpleName.toString())
          ))
        )

        for (mutableEntry in this.asMap()) {
          if (mutableEntry.value.invocationId.name() == metricIds.invocationId.name()) {
            throw MetricNameCollisionException(target, mutableEntry.key, m)
          }
        }

        metricIds
      }
    }
  }

  private fun toMetricId(method: Method, metricNamespace: String, metricName: String): String? {
    val methodName = if (method.parameterCount == 0) method.name else String.format("%s%d",
      method.name, method.parameterCount)
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

    enum class Result {
      SUCCESS, FAILURE;

      override fun toString(): String = name.toLowerCase()
    }
  }
}
