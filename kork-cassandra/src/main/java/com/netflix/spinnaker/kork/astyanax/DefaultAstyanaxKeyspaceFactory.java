/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.astyanax;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.cache.*;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.netflix.astyanax.AstyanaxConfiguration;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.ConnectionPoolConfiguration;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.UnknownException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DefaultAstyanaxKeyspaceFactory implements AstyanaxKeyspaceFactory {

    private final LoadingCache<KeyspaceKey, AstyanaxContext<Keyspace>> keyspaces;

    public DefaultAstyanaxKeyspaceFactory(final AstyanaxConfiguration astyanaxConfiguration,
                                          final ConnectionPoolConfiguration connectionPoolConfiguration,
                                          final KeyspaceConnectionPoolMonitorFactory connectionPoolMonitorFactory,
                                          final ClusterHostSupplierFactory clusterHostSupplierFactory,
                                          final KeyspaceInitializer keyspaceInitializer) {
        keyspaces = CacheBuilder
                .newBuilder()
                .removalListener(createRemovalListener())
                .build(createCacheLoader(astyanaxConfiguration, connectionPoolConfiguration, connectionPoolMonitorFactory, clusterHostSupplierFactory, keyspaceInitializer));
    }

    @Override
    public Keyspace getKeyspace(final String clusterName, final String keyspaceName) throws ConnectionException {
        try {
            return keyspaces.get(new KeyspaceKey(clusterName, keyspaceName)).getClient();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof ConnectionException) {
                throw (ConnectionException) ex.getCause();
            }
            throw new UnknownException(ex.getCause());
        } catch (UncheckedExecutionException uee) {
          uee.printStackTrace();
          throw uee;
        }
    }

    @PreDestroy
    public void shutdownContexts() {
        keyspaces.invalidateAll();
    }

    //
    // visible for testing:
    //
    static final class KeyspaceKey {
        private final String clusterName;
        private final String keyspaceName;

        private KeyspaceKey(String clusterName, String keyspaceName) {
            this.clusterName = Preconditions.checkNotNull(clusterName);
            this.keyspaceName = Preconditions.checkNotNull(keyspaceName);
        }

        public String getClusterName() {
            return clusterName;
        }

        public String getKeyspaceName() {
            return keyspaceName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyspaceKey that = (KeyspaceKey) o;

            if (!clusterName.equals(that.clusterName)) return false;
            if (!keyspaceName.equals(that.keyspaceName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clusterName.hashCode();
            result = 31 * result + keyspaceName.hashCode();
            return result;
        }
    }

    RemovalListener<KeyspaceKey, AstyanaxContext<Keyspace>> createRemovalListener() {
        return new RemovalListener<KeyspaceKey, AstyanaxContext<Keyspace>>() {
            @Override
            public void onRemoval(RemovalNotification<KeyspaceKey, AstyanaxContext<Keyspace>> notification) {
                if (notification.getValue() != null) {
                    notification.getValue().shutdown();
                }
            }
        };
    }

    CacheLoader<KeyspaceKey, AstyanaxContext<Keyspace>> createCacheLoader(final AstyanaxConfiguration astyanaxConfiguration,
                                                                          final ConnectionPoolConfiguration connectionPoolConfiguration,
                                                                          final KeyspaceConnectionPoolMonitorFactory connectionPoolMonitorFactory,
                                                                          final ClusterHostSupplierFactory clusterHostSupplierFactory,
                                                                          final KeyspaceInitializer keyspaceInitializer) {
        return new CacheLoader<KeyspaceKey, AstyanaxContext<Keyspace>>() {
            @Override
            public AstyanaxContext<Keyspace> load(KeyspaceKey key) throws Exception {
                Supplier<List<Host>> hostSupplier = clusterHostSupplierFactory.createHostSupplier(key.getClusterName());
                if (hostSupplier != null) {
                  hostSupplier.get();
                  ((ConnectionPoolConfigurationImpl) connectionPoolConfiguration).setSeeds(null);
                }
                AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
                        .forCluster(key.getClusterName())
                        .forKeyspace(key.getKeyspaceName())
                        .withAstyanaxConfiguration(astyanaxConfiguration)
                        .withConnectionPoolConfiguration(connectionPoolConfiguration)
                        .withConnectionPoolMonitor(connectionPoolMonitorFactory.createMonitorForKeyspace(key.getClusterName(), key.getKeyspaceName()))
                        .withHostSupplier(hostSupplier)
                        .buildKeyspace(ThriftFamilyFactory.getInstance());
                context.start();
                keyspaceInitializer.initKeyspace(context.getClient());
                return context;
            }
        };
    }
}
