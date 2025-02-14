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

package com.netflix.spinnaker.kork.flags.flagd.git

import com.netflix.spinnaker.kork.flags.bindOrCreate
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

/**
 * Handles initialization of a JGit-based feature flag configuration source. Requires that the property
 * `flags.providers.flagd.git.enabled` be set to `true`.
 */
class GitFlagsOpenFeatureInitializer : ApplicationContextInitializer<GenericApplicationContext> {
  override fun initialize(context: GenericApplicationContext) {
    beans {
      val properties = env.bindOrCreate<GitFlagsProperties>("flags.providers.flagd.git")
      if (properties.enabled) {
        requireNotNull(properties.repositoryUri) {
          "The property 'flags.providers.flagd.git.enabled' is true but no repository uri is set"
        }
        requireNotNull(properties.apiToken) {
          "The property 'flags.providers.flagd.git.enabled' is true but no api token is set"
        }
        bean { properties }
        bean<GitRepositoryPoller>()
      }
    }.initialize(context)
  }
}
