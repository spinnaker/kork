/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.kork.jedis;

import java.util.regex.Pattern;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.EvictionPolicy;
import redis.clients.jedis.Jedis;

/**
 * Evicts any Jedis pool connection communicating with a `SLAVE` node.
 *
 * <p>This policy will first use the default eviction policy before running any custom logic.
 */
public class WriteOnlyEvictionPolicy implements EvictionPolicy<Jedis> {

  private static final String REPLICATION_SECTION = "replication";
  private static final Pattern PATTERN = Pattern.compile("role:(.*)\n");

  private final EvictionPolicy<Jedis> defaultEvictionPolicy = new DefaultEvictionPolicy<>();

  @Override
  public boolean evict(EvictionConfig config, PooledObject<Jedis> underTest, int idleCount) {
    boolean evict = defaultEvictionPolicy.evict(config, underTest, idleCount);
    if (evict) {
      return true;
    }

    // Despite Redis itself supporting a `ROLE` command, Jedis itself does not expose a command for
    // this. Thus, we have to go to the general `INFO` command and extract the value manually. :(
    String rawInfo;
    try {
      Jedis connection = underTest.getObject();
      rawInfo = connection.info(REPLICATION_SECTION);
    } catch (Exception e) {
      // Evict any connection where we have trouble getting info.
      return true;
    }

    String role = PATTERN.matcher(rawInfo).group();
    return !"master".equalsIgnoreCase(role);
  }
}
