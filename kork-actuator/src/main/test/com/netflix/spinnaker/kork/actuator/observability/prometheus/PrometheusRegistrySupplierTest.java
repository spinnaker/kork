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

package com.netflix.spinnaker.kork.actuator.observability.prometheus;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;

import com.netflix.spinnaker.kork.actuator.observability.model.MetricsConfig;
import com.netflix.spinnaker.kork.actuator.observability.model.MetricsPrometheusConfig;
import com.netflix.spinnaker.kork.actuator.observability.model.ObservabilityConfigurationProperites;
import io.micrometer.core.instrument.Clock;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PrometheusRegistrySupplierTest {

  MetricsPrometheusConfig prometheusConfig;

  @Mock CollectorRegistry collectorRegistry;

  @Mock Clock clock;

  PrometheusRegistrySupplier sut;

  @Before
  public void before() {
    initMocks(this);
    var observabilityConfigurationProperites = new ObservabilityConfigurationProperites();
    var metricsConfig = new MetricsConfig();
    observabilityConfigurationProperites.setMetrics(metricsConfig);
    prometheusConfig = MetricsPrometheusConfig.builder().build();
    metricsConfig.setPrometheus(prometheusConfig);
    sut =
        new PrometheusRegistrySupplier(
            observabilityConfigurationProperites, collectorRegistry, clock);
  }

  @Test
  public void test_that_get_returns_null_if_prometheus_is_not_enabled() {
    prometheusConfig.setEnabled(false);
    assertNull(sut.get());
  }

  @Test
  public void test_that_get_returns_a_prometheus_registry_if_prometheus_is_enabled() {
    prometheusConfig.setEnabled(true);
    var actual = sut.get();
    assertNotNull(actual);
  }
}
