/*
 * Copyright 2018 Netflix, Inc.
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

import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.kork.jedis.exception.ConflictingConfiguration;
import com.netflix.spinnaker.kork.jedis.exception.MissingRequiredConfiguration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

import java.net.URI;
import java.util.Optional;

public class JedisPoolFactory {

  private GenericObjectPoolConfig poolConfig;
  private RedisConfigurationProperties properties;
  private Registry registry;

  public JedisPoolFactory poolConfig(GenericObjectPoolConfig poolConfig) {
    this.poolConfig = poolConfig;
    return this;
  }

  public JedisPoolFactory properties(RedisConfigurationProperties properties) {
    this.properties = properties;
    return this;
  }

  public JedisPoolFactory registry(Registry registry) {
    this.registry = registry;
    return this;
  }

  public Pool<Jedis> build() {
    if (registry == null) {
      throw new MissingRequiredConfiguration("registry");
    }
    if (properties == null) {
      properties = new RedisConfigurationProperties();
    }
    if (properties.connection == null || "".equals(properties.connection)) {
      throw new MissingRequiredConfiguration("Jedis client must have a connection defined");
    }
    if (properties.sentinel.enabled && properties.cluster.enabled) {
      throw new ConflictingConfiguration("Jedis client cannot have sentinel and cluster flags enabled at the same time");
    }
    if (properties.sentinel.enabled || properties.cluster.enabled) {
      // rz - Added the configuration for Sentinel and Cluster as a carrot for someone to validate these topologies
      // actually work. I figure having the configurations, then exploding before people can actually start services
      // will be better than just not addressing it at all.
      throw new UnsupportedConnectionStrategy("Support has not been verified for Sentinel or Cluster connections");
    }

    URI redisConnection = URI.create(properties.connection);

    String host = redisConnection.getHost();
    int port = redisConnection.getPort() == -1 ? Protocol.DEFAULT_PORT : redisConnection.getPort();
    int database = parseDatabase(redisConnection.getPath());
    String password = parsePassword(redisConnection.getUserInfo());
    GenericObjectPoolConfig objectPoolConfig = Optional.ofNullable(poolConfig).orElse(new GenericObjectPoolConfig());

    return new JedisPool(objectPoolConfig, host, port, properties.timeoutMs, password, database, properties.name);
  }

  private static int parseDatabase(String path) {
    if (path == null) {
      return 0;
    }
    return Integer.parseInt(("/" + Protocol.DEFAULT_DATABASE).split("/", 2)[1]);
  }

  private static String parsePassword(String userInfo) {
    if (userInfo == null) {
      return null;
    }
    return userInfo.split(":", 2)[1];
  }

  static class UnsupportedConnectionStrategy extends IllegalStateException {
    UnsupportedConnectionStrategy(String s) {
      super(s);
    }
  }
}
