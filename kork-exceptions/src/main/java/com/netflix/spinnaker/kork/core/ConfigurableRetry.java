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

package com.netflix.spinnaker.kork.core;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/** Data class supporting configuration binding for retry configuration. */
public class ConfigurableRetry {
  /**
   * flag indicating that retries should be attempted.
   *
   * <p>if false, only one attempt will be made in the {@code retry} method
   */
  private boolean enabled = true;

  /** maximum number of retry attempts */
  private int maxRetries = 3;

  /** interval between attempts */
  private Duration retryBackoff = Duration.ofMillis(50);

  /** whether the retry interval should increase exponentially with each attempt */
  private boolean exponential = false;

  private final RetrySupport retrySupport;

  public ConfigurableRetry() {
    this(new RetrySupport());
  }

  // testing support
  ConfigurableRetry(RetrySupport retrySupport) {
    this.retrySupport = Objects.requireNonNull(retrySupport);
  }

  public <T> T retry(Supplier<T> fn) {
    return retrySupport.retry(fn, enabled ? maxRetries : 1, retryBackoff, exponential);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Duration getRetryBackoff() {
    return retryBackoff;
  }

  public void setRetryBackoff(@Nonnull Duration retryBackoff) {
    this.retryBackoff = Objects.requireNonNull(retryBackoff);
  }

  public boolean isExponential() {
    return exponential;
  }

  public void setExponential(boolean exponential) {
    this.exponential = exponential;
  }

  @Override
  public String toString() {
    return "ConfigurableRetry{"
        + "enabled="
        + enabled
        + ", maxRetries="
        + maxRetries
        + ", retryBackoff="
        + retryBackoff
        + ", exponential="
        + exponential
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConfigurableRetry that = (ConfigurableRetry) o;
    return enabled == that.enabled
        && maxRetries == that.maxRetries
        && exponential == that.exponential
        && retryBackoff.equals(that.retryBackoff);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, maxRetries, retryBackoff, exponential);
  }
}
