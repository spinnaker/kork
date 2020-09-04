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
package com.netflix.spinnaker.kork.sql.config

import javax.sql.DataSource

/**
 * Provides a method of providing your own DataSource if Hikari doesn't suit your needs, while still getting the
 * additional autoconfigure functionality of kork-sql.
 */
interface DataSourceFactory {
  /**
   * Build the [DataSource] from the given configuration.
   *
   * @param poolName The name of the data source
   * @param connectionPoolProperties The properties to configure the [DataSource] with
   */
  fun build(poolName: String, connectionPoolProperties: ConnectionPoolProperties): DataSource
}
