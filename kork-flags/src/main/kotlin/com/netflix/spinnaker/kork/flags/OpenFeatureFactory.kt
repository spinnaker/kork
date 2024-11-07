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
import dev.openfeature.sdk.MutableContext
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.ThreadLocalTransactionContextPropagator
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.stereotype.Component

/**
 * Handles configuration and creation of the [OpenFeatureAPI] singleton. This expects a [FeatureProvider]
 * bean to be available; if one is not, then a no-op implementation is used.
 */
@Component
class OpenFeatureFactory {
  @Bean
  fun openFeatureAPI(provider: ObjectProvider<FeatureProvider>, environment: Environment): OpenFeatureAPI {
    val api = OpenFeatureAPI.getInstance()
    val featureProvider = provider.ifAvailable ?: InMemoryProvider(emptyMap())
    api.setProviderAndWait(featureProvider)
    api.evaluationContext = MutableContext().add("app", environment["spring.application.name"])
    api.transactionContextPropagator = ThreadLocalTransactionContextPropagator()
    return api
  }
}
