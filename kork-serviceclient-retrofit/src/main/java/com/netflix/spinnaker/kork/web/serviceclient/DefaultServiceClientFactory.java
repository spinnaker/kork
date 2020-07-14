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

import static retrofit.Endpoints.newFixedEndpoint;

import com.jakewharton.retrofit.Ok3Client;
import com.netflix.spinnaker.config.DefaultServiceEndpoint;
import com.netflix.spinnaker.config.ServiceEndpoint;
import com.netflix.spinnaker.config.okhttp3.OkHttpClientProvider;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.retrofit.Slf4jRetrofitLogger;
import retrofit.Endpoint;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

@NonnullByDefault
public class DefaultServiceClientFactory implements ServiceClientFactory<RestAdapter.Builder> {

  private final RestAdapter.LogLevel retrofitLogLevel;
  private final OkHttpClientProvider clientProvider;
  private final RequestInterceptor spinnakerRequestInterceptor;

  DefaultServiceClientFactory(
      RestAdapter.LogLevel retrofitLogLevel,
      OkHttpClientProvider clientProvider,
      RequestInterceptor spinnakerRequestInterceptor) {
    this.retrofitLogLevel = retrofitLogLevel;
    this.clientProvider = clientProvider;
    this.spinnakerRequestInterceptor = spinnakerRequestInterceptor;
  }

  @Override
  public <T> T create(Class<T> type, ServiceEndpoint serviceEndpoint) {
    return build(type, serviceEndpoint).build().create(type);
  }

  @Override
  public <T> RestAdapter.Builder build(Class<T> type, ServiceEndpoint serviceEndpoint) {
    Endpoint endpoint = newFixedEndpoint(serviceEndpoint.getBaseUrl());
    return new RestAdapter.Builder()
        .setRequestInterceptor(spinnakerRequestInterceptor)
        .setConverter(new JacksonConverter())
        .setEndpoint(endpoint)
        .setClient(
            new Ok3Client(
                clientProvider.getClient(
                    new DefaultServiceEndpoint(
                        serviceEndpoint.getName(), serviceEndpoint.getBaseUrl()))))
        .setLogLevel(retrofitLogLevel)
        .setLog(new Slf4jRetrofitLogger(type));
  }
}
