/*
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.spinnaker.config

import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties
import com.netflix.spinnaker.retrofit2.SpinnakerRequestInterceptor
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import java.util.concurrent.TimeUnit.MILLISECONDS

@Configuration
@Import(OkHttp3ClientConfiguration::class)
@EnableConfigurationProperties(Retrofit2ConfigurationProperties::class)
class Retrofit2Configuration {

  private val log = LoggerFactory.getLogger(javaClass)

  @Bean(name = ["retrofitClient", "okClient"])
  @Scope(SCOPE_PROTOTYPE)
  fun retrofitClient(
    okHttpClientConfig: OkHttp3ClientConfiguration,
    okHttpClientProperties: OkHttpClientConfigurationProperties,
    interceptors: Set<Interceptor>?
  ): OkHttpClient {
    val cfg = okHttpClientConfig.create().apply {
      interceptors?.forEach {
        log.info("Adding OkHttp Interceptor: ${it.javaClass.simpleName}")
        addNetworkInterceptor(it)
      }

      connectionPool(ConnectionPool(
        okHttpClientProperties.connectionPool.maxIdleConnections,
        okHttpClientProperties.connectionPool.keepAliveDurationMs.toLong(),
        MILLISECONDS
      ))
      retryOnConnectionFailure(okHttpClientProperties.retryOnConnectionFailure)
    }
    return cfg.build()
  }

  @Bean
  fun retrofitLoggingInterceptor(retrofit2ConfigurationProperties: Retrofit2ConfigurationProperties) =
    HttpLoggingInterceptor().apply {
      level = retrofit2ConfigurationProperties.logLevel
    }

  // TODO: this should only be applied to spinnaker-to-spinnaker requests
  @Bean
  fun spinnakerRequestInterceptor(okHttpClientConfigurationProperties: OkHttpClientConfigurationProperties) =
    SpinnakerRequestInterceptor(okHttpClientConfigurationProperties)
}
