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

package com.netflix.spinnaker.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class ServiceConfigurationProperties {

  /** Services to check for health on. */
  private List<String> healthCheckableServices;

  /** Service configuration for internal services. */
  private Map<String, Service> services = new HashMap<>();

  /** Service configurations for external services. */
  private Map<String, Service> integrations = new HashMap<>();

  public List<String> getHealthCheckableServices() {
    return healthCheckableServices;
  }

  public void setHealthCheckableServices(List<String> healthCheckableServices) {
    this.healthCheckableServices = healthCheckableServices;
  }

  public Map<String, Service> getServices() {
    return services;
  }

  public void setServices(Map<String, Service> services) {
    this.services = services;
  }

  public Map<String, Service> getIntegrations() {
    return integrations;
  }

  public void setIntegrations(Map<String, Service> integrations) {
    this.integrations = integrations;
  }

  public static class Service {

    /** Service is enabled if set to true. */
    boolean enabled = true;

    /** Url of the service. */
    String baseUrl;

    /**
     * Shard definitions for the service. If defined, consumers can implement strategies to
     * distribute traffic.
     */
    MultiBaseUrl shards;

    /**
     * Indicates the priority of the configuration that consumers can use to decide for sending
     * traffic.
     */
    int priority = 1;

    /** Additional configuration that is useful while building clients or sending traffic. */
    Map<String, Object> config = new HashMap<>();

    void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public MultiBaseUrl getShards() {
      return shards;
    }

    public void setShards(MultiBaseUrl shards) {
      this.shards = shards;
    }

    public int getPriority() {
      return priority;
    }

    public void setPriority(int priority) {
      this.priority = priority;
    }

    public Map<String, Object> getConfig() {
      return config;
    }

    public void setConfig(Map<String, Object> config) {
      this.config = config;
    }

    static class BaseUrl {

      /** Url of the service. */
      String baseUrl;

      /** Can be used by consumers to prioritize the shard for sending traffic. */
      int priority = 1;

      /** Additional configuration required for the shard. */
      Map<String, Object> config = new HashMap<>();

      BaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
      }
    }

    /** Defines one or more shards. */
    static class MultiBaseUrl {

      /** Set this if no sharding is required. */
      String baseUrl;

      /** One or more shard definitions */
      List<BaseUrl> baseUrls;

      public String getBaseUrl() {
        return baseUrl;
      }

      public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
      }

      public List<BaseUrl> getBaseUrls() {
        return baseUrls;
      }

      public void setBaseUrls(List<BaseUrl> baseUrls) {
        this.baseUrls = baseUrls;
      }
    }

    List<BaseUrl> getBaseUrls() {
      if (shards.baseUrl != null) {
        return Collections.singletonList(new BaseUrl(shards.baseUrl));
      } else if (shards.baseUrls != null) {
        return shards.baseUrls;
      }

      return Collections.singletonList(new BaseUrl(baseUrl));
    }
  }
}
