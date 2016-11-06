/*
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.spinnaker.kork.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;
import java.util.Objects;

public class BootHealthCheckHandler implements HealthCheckHandler, ApplicationListener<ContextRefreshedEvent> {

  private final HealthIndicator aggregateHealth;
  private final ApplicationInfoManager applicationInfoManager;

  public BootHealthCheckHandler(ApplicationInfoManager applicationInfoManager,
                                HealthAggregator aggregator,
                                Map<String, HealthIndicator> healthIndicators) {
    this.applicationInfoManager = Objects.requireNonNull(applicationInfoManager, "applicationInfoManager");
    this.aggregateHealth = new CompositeHealthIndicator(aggregator, healthIndicators);
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    applicationInfoManager.setInstanceStatus(getStatus(InstanceInfo.InstanceStatus.UNKNOWN));
  }

  @Override
  public InstanceInfo.InstanceStatus getStatus(InstanceInfo.InstanceStatus currentStatus) {
    final String statusCode = aggregateHealth.health().getStatus().getCode();
    if (Status.UP.getCode().equals(statusCode)) {
      return InstanceInfo.InstanceStatus.UP;
    } else if (Status.OUT_OF_SERVICE.getCode().equals(statusCode)) {
      return InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
    } else if (Status.DOWN.getCode().equals(statusCode)) {
      return InstanceInfo.InstanceStatus.DOWN;
    } else {
      return InstanceInfo.InstanceStatus.UNKNOWN;
    }
  }
}

