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

import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * An aspect to add to the InvocationHandler invoke lifecycle
 */
interface InvocationAspect<I : InvocationState> {

  /**
   * Determines if the instance supports the specified InvocationState type
   */
  fun supports(invocationState: Class<InvocationState>): Boolean

  /**
   * Creates the InvocationHolder instance, to store state about the invocation
   */
  fun create(target: Any, proxy: Any, method: Method, args: Array<out Any>?, descriptor: SpinnakerPluginDescriptor): I

  /**
   * After method invocation, runs in a finally block
   */
  fun after(success: Boolean, invocationState: I)

  /**
   * If the method ivocation threw an InvocationTargetException, apply some additional processing if
   * desired.
   */
  fun error(e: InvocationTargetException, invocationState: I) { /* default implementation */ }
}
