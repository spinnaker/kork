/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.kork.dynomite;

import com.netflix.dyno.connectionpool.ConnectionPoolConfiguration;
import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.connectionpool.TokenMapSupplier;
import com.netflix.dyno.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.dyno.connectionpool.impl.lb.HostToken;
import com.netflix.dyno.jedis.DynoJedisClient;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LocalRedisDynomiteClient {

  private DynoJedisClient dynoJedisClient;

  public LocalRedisDynomiteClient(int port) {
    String rack = StringUtils.isBlank(System.getenv("EC2_REGION")) ? "local" : System.getenv("EC2_REGION");
    HostSupplier localHostSupplier = new HostSupplier() {
      final Host hostSupplierHost = new Host("localhost", rack, Host.Status.Up);

      @Override
      public List<Host> getHosts() {
        return Collections.singletonList(hostSupplierHost);
      }
    };

    TokenMapSupplier tokenMapSupplier = new TokenMapSupplier() {
      final Host tokenHost = new Host("localhost", port, rack, Host.Status.Up);
      final HostToken localHostToken = new HostToken(100000L, tokenHost);

      @Override
      public List<HostToken> getTokens(Set<Host> activeHosts) {
        return Collections.singletonList(localHostToken);
      }

      @Override
      public HostToken getTokenForHost(Host host, Set<Host> activeHosts) {
        return localHostToken;
      }
    };

    this.dynoJedisClient = new DynoJedisClient.Builder()
      .withDynomiteClusterName("local")
      .withApplicationName(String.valueOf(port))
      .withHostSupplier(localHostSupplier)
      .withCPConfig(
        new ConnectionPoolConfigurationImpl(String.valueOf(port))
          .setCompressionStrategy(ConnectionPoolConfiguration.CompressionStrategy.NONE)
          .setLocalRack(rack)
          .withHashtag("{}")
          .withHostSupplier(localHostSupplier)
          .withTokenSupplier(tokenMapSupplier)
      )
      .build();
  }

  public DynoJedisClient getClient() {
    return dynoJedisClient;
  }
}
