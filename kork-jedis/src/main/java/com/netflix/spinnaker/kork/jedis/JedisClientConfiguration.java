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
package com.netflix.spinnaker.kork.jedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Import(RedisClientConfiguration.class)
public class JedisClientConfiguration {

  @Bean
  JedisClientDelegateFactory jedisClientDelegateFactory(ObjectMapper objectMapper) {
    return new JedisClientDelegateFactory(objectMapper);
  }

  @Bean
  public List<HealthIndicator> jedisClientHealthIndicators(List<RedisClientDelegate> redisClientDelegates) {
    return redisClientDelegates.stream()
      .filter(it -> it instanceof JedisClientDelegate)
      .map(it -> JedisHealthIndicatorFactory.build((JedisClientDelegate) it))
      .collect(Collectors.toList());
  }
}
