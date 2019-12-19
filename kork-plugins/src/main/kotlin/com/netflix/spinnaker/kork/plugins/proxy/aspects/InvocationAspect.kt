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
 * An aspect to use with [ExtensionInvocationProxy], allows for storing state about an invocation
 * and accessing that state on [error] and [after] the invocation.
 */
interface InvocationAspect<I : InvocationState> {

  /**
   * Determines if the instance supports the specified [InvocationState] type.
   *
   * @param invocationState The [InvocationState] type that this aspect supports
   */
  fun supports(invocationState: Class<InvocationState>): Boolean

  /**
   * The params [proxy], [method], and [args] are documented at [java.lang.reflect.InvocationHandler]
   *
   * @param target The target object that is being proxied
   * @param descriptor The [SpinnakerPluginDescriptor] provides metadata about the plugin
   *
   * @return I The [InvocationState] instance, which is used to store state about the invocation.
   */
  fun createState(target: Any, proxy: Any, method: Method, args: Array<out Any>?, descriptor: SpinnakerPluginDescriptor): I

  /**
   * After method invocation, runs in a finally block.
   *
   * @param success If the method was invoked without exception this will be `true`, otherwise `false`
   * @param invocationState The state object created via [createState]
   */
  fun after(success: Boolean, invocationState: I)

  /**
   * If the method invocation threw an InvocationTargetException, apply some additional processing if
   * desired.
   *
   * @param e InvocationTargetException which is thrown via
   * @param invocationState The invocationState object created via [createState]
   */
  fun error(e: InvocationTargetException, invocationState: I) { /* default implementation */ }
}
