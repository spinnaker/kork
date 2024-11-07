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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

/**
 * Configuration properties related to using git polling for a source of flagd data.
 *
 * @param enabled toggles this flag source backend
 * @param repositoryUri git repository URI to clone from (required)
 * @param branch branch to check out (default of HEAD)
 * @param apiToken authentication token to use (required)
 * @param pollingInterval how often to poll git for updates (default of every 5 minutes)
 * @param flagsSource name of file in the git repository to load feature flags from (default is `spinnaker.json`)
 */
@ConfigurationProperties("flags.providers.flagd.git")
@ConstructorBinding
data class GitFlagsProperties(
  @DefaultValue("false") val enabled: Boolean,
  val repositoryUri: String,
  @DefaultValue("HEAD") val branch: String,
  val apiToken: String,
  @DefaultValue("PT5M") val pollingInterval: Duration,
  @DefaultValue("spinnaker.json") val flagsSource: String,
)
