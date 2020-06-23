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

package com.netflix.spinnaker.kork.plugins.api.environment;

import com.netflix.spinnaker.kork.annotations.Beta;
import javax.annotation.Nonnull;

@Beta
public interface Environment {

  /**
   * Returns the environment name specified at:
   *
   * <pre>{@code spinnaker.extensibility.environment.name}</pre>
   *
   * If a name is not specified, returns the last active Spring profile.
   *
   * <p>If the environment name is not configured and there are no active spring profiles, returns
   * the default value.
   *
   * @return String
   */
  @Nonnull
  default String getName() {
    return "default";
  }

  /**
   * Return the environment region specified at:
   *
   * <pre>{@code spinnaker.extensibility.environment.region}</pre>
   *
   * If a region is not specified, an environment variable from which to source the region may be
   * specified:
   *
   * <pre>{@code spinnaker.extensibility.environment.regionVariable}</pre>
   *
   * If there is no region environment variable and no specific region configured, returns the
   * default value.
   *
   * @return String
   */
  @Nonnull
  default String getRegion() {
    return "default";
  }
}
