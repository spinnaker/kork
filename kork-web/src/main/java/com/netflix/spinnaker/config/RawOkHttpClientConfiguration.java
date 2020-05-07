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
 */

package com.netflix.spinnaker.config;

import com.netflix.spinnaker.okhttp.OkHttp3MetricsInterceptor;
import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OkHttpClientConfigurationProperties.class)
class RawOkHttpClientConfiguration {

  /** OkHttpClient instance to be reused it for all HTTP calls. */
  @Bean
  OkHttpClient okHttpClient(
      OkHttpClientConfigurationProperties okHttpClientConfigurationProperties,
      OkHttp3MetricsInterceptor okHttp3MetricsInterceptor) {
    return new RawOkHttpClientFactory()
        .create(okHttpClientConfigurationProperties, okHttp3MetricsInterceptor);
  }
}
