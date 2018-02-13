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

public class RedisConfigurationProperties {
  public String name;
  public String connection;
  public int timeoutMs = 2000;

  public SentinelProperties sentinel = new SentinelProperties();
  public ClusterProperties cluster = new ClusterProperties();

  public static class SentinelProperties {
    public boolean enabled = false;
  }

  public static class ClusterProperties {
    public boolean enabled = false;
  }
}
