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
package com.netflix.spinnaker.metrics;

import com.netflix.spectator.api.*;
import com.netflix.spinnaker.config.SpectatorEndpointConfigurationProperties;
import com.netflix.spinnaker.metrics.filter.PrototypeMeasurementFilter;
import com.netflix.spinnaker.metrics.filter.TagMeasurementFilter;
import com.netflix.spinnaker.metrics.model.ApplicationRegistry;
import com.netflix.spinnaker.metrics.model.MetricValues;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This class has been ported spectator-web-spring (Spring Boot 1.x only) to Spring Boot 2 compatibility.
 */
@Endpoint(id = MetricsEndpoint.ID)
public class MetricsEndpoint {

  /**
   * A measurement filter that accepts all measurements.
   */
  public static final Predicate<Measurement> ALL_MEASUREMENTS_FILTER = m -> true;

  public static final String ID = "spectator-metrics";

  private final Registry registry;
  private final SpectatorEndpointConfigurationProperties properties;

  private final Map<Id, String> knownMeterKinds = new HashMap<>();
  private Predicate<Measurement> defaultMeasurementFilter = null;

  public MetricsEndpoint(Registry registry,
                         SpectatorEndpointConfigurationProperties properties) {
    this.registry = registry;
    this.properties = properties;
  }

  /**
   * The default measurement filter is configured through properties.
   */
  public Predicate<Measurement> getDefaultMeasurementFilter() throws IOException {
    if (defaultMeasurementFilter != null) {
      return defaultMeasurementFilter;
    }

    if (!properties.getWebEndpoint().getPrototypeFilter().getPath().isEmpty()) {
      defaultMeasurementFilter = PrototypeMeasurementFilter.loadFromPath(
        properties.getWebEndpoint().getPrototypeFilter().getPath());
    } else {
      defaultMeasurementFilter = ALL_MEASUREMENTS_FILTER;
    }

    return defaultMeasurementFilter;
  }


  /**
   * Endpoint for querying current metric values.
   *
   * The result is a JSON document describing the metrics and their values.
   * The result can be filtered using query parameters or configuring the
   * controller instance itself.
   */
  @ReadOperation
  public ApplicationRegistry getMetrics(@RequestParam Map<String, String> filters)
    throws IOException {
    boolean all = filters.get("all") != null;
    String filterMeterNameRegex = filters.getOrDefault("meterNameRegex", "");
    String filterTagNameRegex = filters.getOrDefault("tagNameRegex", "");
    String filterTagValueRegex = filters.getOrDefault("tagValueRegex", "");
    TagMeasurementFilter queryFilter = new TagMeasurementFilter(
      filterMeterNameRegex, filterTagNameRegex, filterTagValueRegex);
    Predicate<Measurement> filter;
    if (all) {
      filter = queryFilter;
    } else {
      filter = queryFilter.and(getDefaultMeasurementFilter());
    }

    ApplicationRegistry response = new ApplicationRegistry();
    response.setApplicationName(properties.getApplicationName());
    response.setApplicationVersion(properties.getApplicationVersion());
    response.setMetrics(encodeRegistry(registry, filter));
    return response;
  }

  /**
   * Internal API for encoding a registry that can be encoded as JSON.
   * This is a helper function for the REST endpoint and to test against.
   */
  Map<String, MetricValues> encodeRegistry(
    Registry sourceRegistry, Predicate<Measurement> filter) {
    Map<String, MetricValues> metricMap = new HashMap<>();

    /*
     * Flatten the meter measurements into a map of measurements keyed by
     * the name and mapped to the different tag variants.
     */
    for (Meter meter : sourceRegistry) {
      String kind = knownMeterKinds.computeIfAbsent(
        meter.id(), k -> meterToKind(sourceRegistry, meter));

      for (Measurement measurement : meter.measure()) {
        if (!filter.test(measurement)) {
          continue;
        }
        if (Double.isNaN(measurement.value())) {
          continue;
        }

        String meterName = measurement.id().name();
        MetricValues have = metricMap.get(meterName);
        if (have == null) {
          metricMap.put(meterName, new MetricValues(kind, measurement));
        } else {
          have.addMeasurement(measurement);
        }
      }
    }
    return metricMap;
  }

  /**
   * Determine the type of a meter for reporting purposes.
   *
   * @param registry
   *    Used to provide supplemental information (e.g. to search for the meter).
   *
   * @param meter
   *    The meters whose kind we want to know.
   *
   * @return
   *    A string such as "Counter". If the type cannot be identified as one of
   *    the standard Spectator api interface variants, then the simple class name
   *    is returned.
   */
  public static String meterToKind(Registry registry, Meter meter) {
    String kind;
    if (meter instanceof Timer) {
      kind = "Timer";
    } else if (meter instanceof Counter) {
      kind = "Counter";
    } else if (meter instanceof Gauge) {
      kind = "Gauge";
    } else if (meter instanceof DistributionSummary) {
      kind = "DistributionSummary";
    } else {
      kind = meter.getClass().getSimpleName();
    }
    return kind;
  }
}
