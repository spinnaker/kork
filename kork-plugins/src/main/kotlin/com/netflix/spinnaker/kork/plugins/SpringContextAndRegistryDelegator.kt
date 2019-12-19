/*
 * Copyright 2019 Armory, Inc.
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

package com.netflix.spinnaker.kork.plugins

import com.netflix.spinnaker.kork.plugins.api.spring.SpringContextAndRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigRegistry

class SpringContextAndRegistryDelegator(
  private val configurableApplicationContext: ConfigurableApplicationContext,
  private val annotationConfigRegistry: AnnotationConfigRegistry
) :
  SpringContextAndRegistry,
  ConfigurableApplicationContext by configurableApplicationContext,
  AnnotationConfigRegistry by annotationConfigRegistry {

  companion object {
    operator fun <T> invoke(ctx: T): SpringContextAndRegistryDelegator
      where T : ConfigurableApplicationContext,
            T : AnnotationConfigRegistry {
      return SpringContextAndRegistryDelegator(ctx, ctx)
    }
  }
}
