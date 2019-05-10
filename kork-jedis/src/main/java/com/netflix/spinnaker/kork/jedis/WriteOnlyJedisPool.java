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

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * A custom JedisPool implementation designed for primarily write-only workloads, with an aggressive
 * eviction policy to recycle connections as quickly as possible when a failover is detected.
 */
public class WriteOnlyJedisPool extends JedisPool {

  @Override
  public void initPool(GenericObjectPoolConfig poolConfig, PooledObjectFactory<Jedis> factory) {
    if (internalPool != null) {
      try {
        closeInternalPool();
      } catch (Exception e) {
      }
    }

    internalPool = new GenericObjectPool<Jedis>(factory, poolConfig);
    internalPool.setTestWhileIdle(true);
    internalPool.setTestOnBorrow(true);
    internalPool.setEvictionPolicy(new WriteOnlyEvictionPolicy());
  }
}
