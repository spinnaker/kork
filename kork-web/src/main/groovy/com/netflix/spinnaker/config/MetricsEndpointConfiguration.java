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
package com.netflix.spinnaker.config;

import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@ConditionalOnClass(Registry.class)
@EnableConfigurationProperties(SpectatorEndpointConfigurationProperties.class)
public class MetricsEndpointConfiguration extends WebSecurityConfigurerAdapter {

  @Bean
  @ConditionalOnExpression("${spectator.web-endpoint.enabled:false}")
  public MetricsEndpoint spectatorMetricsEndpoint(Registry registry,
                                                  SpectatorEndpointConfigurationProperties properties) {
    return new MetricsEndpoint(registry, properties);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Allow anyone to access the metrics endpoints.
    http.requestMatcher(EndpointRequest.to(MetricsEndpoint.ID)).authorizeRequests().anyRequest().permitAll();
  }
}
