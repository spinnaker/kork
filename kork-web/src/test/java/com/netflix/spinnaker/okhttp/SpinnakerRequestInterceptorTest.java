/*
 * Copyright 2024 Salesforce, Inc.
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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.netflix.spinnaker.kork.common.Header;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;

@WireMockTest
class SpinnakerRequestInterceptorTest {

  public static final String REQUEST_PATH = "/foo";

  public static final String TEST_USER = "some-user";

  public static final String TEST_ACCOUNTS = "some-accounts";

  public static final String TEST_REQUEST_ID = "some-request-id";

  public static final Map<Header, String> TEST_SPINNAKER_HEADERS =
      Map.of(
          Header.USER,
          TEST_USER,
          Header.ACCOUNTS,
          TEST_ACCOUNTS,
          Header.REQUEST_ID,
          TEST_REQUEST_ID);

  @BeforeEach
  void setup(TestInfo testInfo, WireMockRuntimeInfo wmRuntimeInfo) {
    System.out.println("--------------- Test " + testInfo.getDisplayName());

    WireMock wireMock = wmRuntimeInfo.getWireMock();

    // set up an arbitrary response to avoid 404s, and so it's possible to
    // verify the request headers wiremock receives.
    wireMock.register(get(REQUEST_PATH).willReturn(ok()));
  }

  @AfterEach
  void cleanup() {
    AuthenticatedRequest.clear();
  }

  @Test
  void propagateSpinnakerHeadersFalse(WireMockRuntimeInfo wmRuntimeInfo) {
    OkHttpClientConfigurationProperties okHttpClientConfigurationProperties =
        new OkHttpClientConfigurationProperties();
    okHttpClientConfigurationProperties.setPropagateSpinnakerHeaders(false);
    SpinnakerRequestInterceptor spinnakerRequestInterceptor =
        new SpinnakerRequestInterceptor(okHttpClientConfigurationProperties);

    RetrofitService retrofitService =
        makeRetrofitService(wmRuntimeInfo, spinnakerRequestInterceptor);

    // Add some spinnaker headers to the MDC
    TEST_SPINNAKER_HEADERS.forEach(AuthenticatedRequest::set);

    // Make a request
    retrofitService.getRequest();

    // Verify that wiremock didn't receive the spinnaker headers
    TEST_SPINNAKER_HEADERS.forEach(
        (Header header, String value) -> {
          verify(getRequestedFor(urlPathEqualTo(REQUEST_PATH)).withoutHeader(header.getHeader()));
        });
  }

  @Test
  void propagateSpinnakerHeadersTrue(WireMockRuntimeInfo wmRuntimeInfo) {
    OkHttpClientConfigurationProperties okHttpClientConfigurationProperties =
        new OkHttpClientConfigurationProperties();
    okHttpClientConfigurationProperties.setPropagateSpinnakerHeaders(true);
    SpinnakerRequestInterceptor spinnakerRequestInterceptor =
        new SpinnakerRequestInterceptor(okHttpClientConfigurationProperties);

    RetrofitService retrofitService =
        makeRetrofitService(wmRuntimeInfo, spinnakerRequestInterceptor);

    // Add some spinnaker headers to the MDC
    TEST_SPINNAKER_HEADERS.forEach(AuthenticatedRequest::set);

    // Make a request
    retrofitService.getRequest();

    // Verify that wiremock received the spinnaker headers
    TEST_SPINNAKER_HEADERS.forEach(
        (Header header, String value) -> {
          verify(
              getRequestedFor(urlPathEqualTo(REQUEST_PATH))
                  .withHeader(header.getHeader(), equalTo(value)));
        });
  }

  private RetrofitService makeRetrofitService(
      WireMockRuntimeInfo wmRuntimeInfo, SpinnakerRequestInterceptor spinnakerRequestInterceptor) {
    return new RestAdapter.Builder()
        .setRequestInterceptor(spinnakerRequestInterceptor)
        .setEndpoint(wmRuntimeInfo.getHttpBaseUrl())
        .build()
        .create(RetrofitService.class);
  }

  interface RetrofitService {
    @GET(REQUEST_PATH)
    Response getRequest();
  }
}
