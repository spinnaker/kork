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

package com.netflix.spinnaker.kork.plugins.proxy

import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import com.netflix.spinnaker.kork.plugins.proxy.aspects.InvocationHolder
import com.netflix.spinnaker.kork.plugins.proxy.aspects.InvocationAspect
import java.lang.RuntimeException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class ExtensionInvocationProxy(
  private val target: Any,
  private val invocationAspects: List<InvocationAspect<InvocationHolder>>,
  private val pluginDescriptor: SpinnakerPluginDescriptor
) : InvocationHandler {

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
    val invocationHolders: MutableSet<InvocationHolder> = mutableSetOf()
    invocationHolders.create(proxy, method, args)

    val result: Any
    var success = false
    try {
      result = method.invoke(target, *(args ?: arrayOfNulls<Any>(0)))
      success = true
    } catch (e: InvocationTargetException) {
      invocationHolders.error(e)
      throw e.cause ?: RuntimeException("Caught invocation target exception without cause.", e)
    } finally {
      invocationHolders.after(success)
    }

    return result
  }

  companion object {
    fun proxy(
      target: Any,
      invocationAspects: List<InvocationAspect<InvocationHolder>>,
      descriptor: SpinnakerPluginDescriptor
    ): Any {
      return Proxy.newProxyInstance(
        target.javaClass.classLoader,
        target.javaClass.interfaces,
        ExtensionInvocationProxy(target, invocationAspects, descriptor)
      )
    }
  }

  private fun MutableSet<InvocationHolder>.create(proxy: Any, method: Method, args: Array<out Any>?) {
    invocationAspects.forEach {
      this.add(it.create(target, proxy, method, args, pluginDescriptor))
    }
  }

  private fun MutableSet<InvocationHolder>.error(e: InvocationTargetException) {
    this.forEach { holder ->
      invocationAspects.find {
        it.supports(holder.javaClass)
      }?.error(e, holder)
    }
  }

  private fun MutableSet<InvocationHolder>.after(success: Boolean) {
    this.forEach { holder ->
      invocationAspects.find {
        it.supports(holder.javaClass)
      }?.after(success, holder)
    }
  }
}
