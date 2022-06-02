/*
 * Copyright 2022 Apple Inc.
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

package com.netflix.spinnaker.kork.secrets.user;

import com.netflix.spinnaker.kork.annotations.Beta;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provides configuration properties related to user secrets caching. */
@ConfigurationProperties(prefix = "caching.user-secrets")
@Data
@NonnullByDefault
@Beta
public class UserSecretCacheProperties {
  /**
   * Maximum number of UserSecret entries to cache. The actual number of items in this cache may be
   * slightly more or less than the configured value as cache items are evicted.
   */
  private long maximumCachedItems = 50_000L;

  /**
   * Length of time a cached UserSecret is valid for after it is first cached. After this time, the
   * cache item is evicted and forced to be refreshed regardless of how recently it was last
   * accessed.
   */
  private Duration expireAfterWrite = Duration.ofMinutes(60);

  /**
   * Length of time a cached UserSecret is valid for after it is last accessed. User secrets that
   * haven't been accessed for longer than this duration will be evicted.
   */
  private Duration expireAfterAccess = Duration.ofMinutes(20);
}
