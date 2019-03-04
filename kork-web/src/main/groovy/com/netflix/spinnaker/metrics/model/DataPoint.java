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
package com.netflix.spinnaker.metrics.model;

import com.netflix.spectator.api.Measurement;

import java.util.Objects;

public class DataPoint {
  /**
   * Factory method to create a DataPoint from a Measurement.
   */
  public static DataPoint make(Measurement m) {
    return new DataPoint(m.timestamp(), m.value());
  }

  private final long timestamp;
  private final double value;

  /**
   * The measurement timestamp.
   */
  public long getT() {
    return timestamp;
  }

  /**
   * The measurement value.
   */
  public double getV() {
    return value;
  }

  /**
   * Constructor.
   */
  public DataPoint(long timestamp, double value) {
    this.timestamp = timestamp;
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("t=%d, v=%f", timestamp, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DataPoint)) return false;
    DataPoint other = (DataPoint) obj;
    return timestamp == other.timestamp && (Math.abs(value - other.value) < 0.001);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, value);
  }
}
