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
package com.netflix.spinnaker.kork.models

import com.google.protobuf.StringValue
import com.google.protobuf.Timestamp
import java.time.Instant

/**
 * Converts a [Timestamp] into an [Instant].
 */
fun Timestamp.toInstant(): Instant =
  Instant.ofEpochSecond(seconds, nanos.toLong())

/**
 * Converts an [Instant] into a [Timestamp].
 */
fun Instant.toTimestamp(): Timestamp =
  Timestamp.newBuilder()
    .setSeconds(epochSecond)
    .setNanos(nano)
    .build()

/**
 * Converts a [String] to the Nullable Type [StringValue].
 */
fun String.toProto(): StringValue =
  StringValue.of(this)
