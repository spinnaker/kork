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

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays

internal fun Method.isAllowed(): Boolean {
  // Only public methods and ignore any methods from the root Object class
  return (
    Modifier.isPublic(this.modifiers) ||
      Arrays.stream(Any::class.java.declaredMethods)
        .noneMatch { m: Method -> m.name == this.name }
    )
}
