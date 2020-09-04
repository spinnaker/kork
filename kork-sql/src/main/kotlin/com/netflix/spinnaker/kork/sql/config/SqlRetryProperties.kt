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
package com.netflix.spinnaker.kork.sql.config

/**
 * Retry config for SQL.
 *
 * @param transactions Defines retry configs for write operations
 * @param reads Defines retry configs for read operations
 */
data class SqlRetryProperties(
  var transactions: RetryProperties = RetryProperties(),
  var reads: RetryProperties = RetryProperties()
)

/**
 * Simple retry configuration.
 *
 * @param maxRetries The maximum number of retries in the event of an error
 * @param backoffMs The amount of time to wait between retries
 */
data class RetryProperties(
  var maxRetries: Int = 5,
  var backoffMs: Long = 100
)
