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

package com.netflix.spinnaker.kork.web.serviceclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.config.okhttp3.OkHttpClientProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

@Configuration
@ConditionalOnProperty(value = "retrofit.enabled", matchIfMissing = true)
public class RetrofitServiceFactoryAutoConfiguration {

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  ServiceClientFactory serviceClientFactory(
      RestAdapter.LogLevel retrofitLogLevel,
      OkHttpClientProvider clientProvider,
      RequestInterceptor spinnakerRequestInterceptor) {
    return new DefaultRetrofitServiceFactory(
        retrofitLogLevel, clientProvider, spinnakerRequestInterceptor);
  }

  @Bean
  RetrofitServiceProvider serviceClientProvider(
      List<ServiceClientFactory> serviceClientFactories, ObjectMapper objectMapper) {
    return new RetrofitServiceProvider(serviceClientFactories, objectMapper);
  }
}
