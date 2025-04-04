/*
 * Copyright 2025 OpsMx, Inc.
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

package com.netflix.spinnaker.okhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jakewharton.retrofit.Ok3Client;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spinnaker.config.OkHttp3ClientConfiguration;
import com.netflix.spinnaker.config.OkHttpMetricsInterceptorProperties;
import com.netflix.spinnaker.config.okhttp3.DefaultOkHttpClientBuilderProvider;
import com.netflix.spinnaker.config.okhttp3.OkHttpClientProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit.RetrofitError;
import retrofit.http.GET;
import retrofit.http.Query;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {
      OkHttpClient.class,
      OkHttpClientConfigurationProperties.class,
      OkHttpClientProvider.class,
      DefaultOkHttpClientBuilderProvider.class,
      Retrofit2EncodeCorrectionInterceptor.class,
      Retrofit1ImpactTest.TestConfiguration.class,
      OkHttpMetricsInterceptorProperties.class,
      OkHttp3MetricsInterceptor.class,
      OkHttp3ClientConfiguration.class,
      NoopRegistry.class
    })
public class Retrofit1ImpactTest {

  private static final String QUERY_PARAM_VAL = "qry_with space";
  private static final String ENCODED_QUERY_PARAM_VAL =
      encodedString(QUERY_PARAM_VAL); // qry_with+space

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired OkHttp3ClientConfiguration clientConfiguration;

  @Test
  public void test_Retrofit2Interceptors_Impact_On_Retrofit1() {
    wireMock.stubFor(get("/test?qry=" + ENCODED_QUERY_PARAM_VAL).willReturn(ok()));
    Retrofit1Service retrofit1Service = getRetrofit1Service(wireMock.baseUrl());

    Throwable thrown = catchThrowable(() -> retrofit1Service.get(QUERY_PARAM_VAL));
    assertThat(thrown).isInstanceOf(RetrofitError.class);
    RetrofitError retrofitError = (RetrofitError) thrown;

    // With this encoding change, the request doesn't match the stub configured
    // above, so wiremock responds with 404/not found.
    assertThat(retrofitError.getResponse().getStatus()).isEqualTo(404);

    LoggedRequest requestReceived = wireMock.getAllServeEvents().get(0).getRequest();
    String receivedUrl = requestReceived.getUrl();

    // Retrofit2EncodeCorrectionInterceptor encodes + with %2B
    assertThat(receivedUrl).isEqualTo("/test?qry=qry_with%2Bspace");
  }

  private static String encodedString(String input) {
    return URLEncoder.encode(input, StandardCharsets.UTF_8);
  }

  private Retrofit1Service getRetrofit1Service(String baseUrl) {
    // clientConfiguration.create() is for use with retrofit1.  The point of
    // this test is to verify that clientConfiguration.create doesn't include
    // the Retrofit2EncodeCorrectionInterceptor since that's only for retrofit2.
    return new retrofit.RestAdapter.Builder()
        .setEndpoint(baseUrl)
        .setClient(new Ok3Client(clientConfiguration.create().build()))
        .build()
        .create(Retrofit1Service.class);
  }

  @Configuration
  public static class TestConfiguration {

    @Bean
    public HttpLoggingInterceptor.Level logLevel() {
      return HttpLoggingInterceptor.Level.BASIC;
    }

    @Bean
    public SpinnakerRequestHeaderInterceptor spinnakerRequestHeaderInterceptor() {
      return new SpinnakerRequestHeaderInterceptor(true);
    }
  }

  interface Retrofit1Service {
    @GET("/test")
    Void get(@Query(value = "qry") String qry);
  }
}
