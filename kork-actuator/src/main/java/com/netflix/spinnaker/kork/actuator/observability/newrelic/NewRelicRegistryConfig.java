/*
 * Copyright 2025 Netflix, Inc.
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
 */

package com.netflix.spinnaker.kork.actuator.observability.newrelic;

import com.netflix.spinnaker.kork.actuator.observability.model.MetricsNewRelicConfig;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * New Relic config wrapper that sources its config from the Spring Context Plugin Configuration.
 */
public class NewRelicRegistryConfig implements io.micrometer.NewRelicRegistryConfig {

  private final MetricsNewRelicConfig newRelicConfig;

  public NewRelicRegistryConfig(MetricsNewRelicConfig newRelicConfig) {
    this.newRelicConfig = newRelicConfig;
  }

  @Override
  public String get(String key) {
    return null; // NOOP, source config from the PluginConfig that is injected
  }

  @Override
  public String apiKey() {
    return Optional.ofNullable(newRelicConfig.getApiKey())
        .orElseThrow(
            () ->
                new NewRelicRegistryConfigException(
                    "The New Relic API key is a required plugin config property"));
  }

  @Override
  public String uri() {
    return newRelicConfig.getUri();
  }

  @Override
  public String serviceName() {
    return null;
  }

  @Override
  public boolean enableAuditMode() {
    return newRelicConfig.isEnableAuditMode();
  }

  @Override
  public Duration step() {
    return Duration.of(newRelicConfig.getStepInSeconds(), ChronoUnit.SECONDS);
  }

  @Override
  public int numThreads() {
    return newRelicConfig.getNumThreads();
  }

  @Override
  public int batchSize() {
    return newRelicConfig.getBatchSize();
  }

  public static class NewRelicRegistryConfigException extends RuntimeException {
    public NewRelicRegistryConfigException(String message) {
      super(message);
    }
  }
}
