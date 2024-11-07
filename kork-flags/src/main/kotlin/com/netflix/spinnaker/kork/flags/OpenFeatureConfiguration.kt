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

import org.springframework.context.annotation.Configuration

/**
 * Annotations a [Configuration] class that sets up OpenFeature-related beans. This works similar to an
 * autoconfiguration but using eager imports rather than deferred.
 *
 * OpenFeature configurations should conditionally create a [dev.openfeature.sdk.FeatureProvider] bean
 * when enabled or participate with another configuration that does.
 */
@Configuration(proxyBeanMethods = false)
@Target(AnnotationTarget.CLASS)
annotation class OpenFeatureConfiguration
