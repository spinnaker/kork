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

import java.time.Duration

/**
 * Configuration properties related to using git polling for a source of flagd data.
 *
 * @property enabled toggles this flag source backend
 * @property repositoryUri git repository URI to clone from (required)
 * @property branch branch to check out (default of HEAD)
 * @property apiToken authentication token to use (required)
 * @property pollingInterval how often to poll git for updates (default of every 5 minutes)
 * @property flagsSource name of file in the git repository to load feature flags from (default is `spinnaker.json`)
 */
class GitFlagsProperties {
  var enabled: Boolean = false
  var repositoryUri: String? = null
  var branch: String = "HEAD"
  var apiToken: String? = null
  var pollingInterval: Duration = Duration.ofMinutes(5)
  var flagsSource: String = "spinnaker.json"
}
