/*
 * Copyright 2024 Apple, Inc.
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

package com.netflix.spinnaker.kork.flags

import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.Features
import dev.openfeature.sdk.MutableContext
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.ThreadLocalTransactionContextPropagator
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.core.env.get

/**
 * Handles initialization of an [OpenFeatureAPI] and [Features] bean using a configured [FeatureProvider]
 * bean if available. Other flags modules should register their beans via an [ApplicationContextInitializer].
 */
class OpenFeatureCoreInitializer : ApplicationContextInitializer<GenericApplicationContext> {
  override fun initialize(context: GenericApplicationContext) {
    beans {
      bean<OpenFeatureAPI> {
        val provider = provider<FeatureProvider>().ifUnique ?: InMemoryProvider(emptyMap())
        OpenFeatureAPI.getInstance().also { api ->
          api.setProviderAndWait(provider)
          api.evaluationContext = MutableContext().add("app", env["spring.application.name"])
          api.transactionContextPropagator = ThreadLocalTransactionContextPropagator()
        }
      }
      bean<Features> {
        ref<OpenFeatureAPI>().client
      }
    }.initialize(context)
  }
}
