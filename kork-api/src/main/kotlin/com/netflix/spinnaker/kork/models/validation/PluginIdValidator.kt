/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.spinnaker.kork.models.validation

import com.netflix.spinnaker.kork.models.validation.constraints.PluginId
import java.util.regex.Pattern
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

/**
 * Validates that a [String] value conforms to the canonical [PluginId] format.
 */
class PluginIdValidator : ConstraintValidator<PluginId, String> {
  override fun isValid(value: String, context: ConstraintValidatorContext): Boolean {
    return pattern.matcher(value).let { matcher ->
      if (!matcher.matches()) {
        context
          .buildConstraintViolationWithTemplate("Plugin IDs must follow a '{namespace}.{id}' format ($pattern)")
          .addConstraintViolation()
        false
      } else {
        true
      }
    }
  }

  companion object {
    private val pattern = Pattern.compile("^(?<namespace>[\\w\\-.])+\\.(?<id>[\\w\\-]+)$")
  }
}
