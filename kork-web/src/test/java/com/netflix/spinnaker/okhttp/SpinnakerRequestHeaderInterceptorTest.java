/*
 * Copyright 2023 OpsMx, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.spinnaker.kork.common.Header;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SpinnakerRequestHeaderInterceptorTest {

  MockedStatic<AuthenticatedRequest> mockAuthenticatedRequest;

  @BeforeEach
  void setup() {
    mockAuthenticatedRequest = Mockito.mockStatic(AuthenticatedRequest.class);
  }

  @AfterEach
  void cleanup() {
    mockAuthenticatedRequest.close();
  }

  @Test
  void requestContainsAuthorizationHeader() throws Exception {
    // given
    Map<String, Optional<String>> authHeaders = new HashMap<String, Optional<String>>() {};
    authHeaders.put(Header.USER.getHeader(), Optional.of("some user"));
    authHeaders.put(Header.ACCOUNTS.getHeader(), Optional.of("Some ACCOUNTS"));
    OkHttpClientConfigurationProperties okHttpClientConfigurationProperties =
        new OkHttpClientConfigurationProperties();
    SpinnakerRequestHeaderInterceptor headerInterceptor =
        new SpinnakerRequestHeaderInterceptor(okHttpClientConfigurationProperties);
    Interceptor.Chain chain = mock(Interceptor.Chain.class);

    when(chain.request())
        .thenReturn(new Request.Builder().url("http://1.1.1.1/heath-check").build());

    when(AuthenticatedRequest.getAuthenticationHeaders()).thenReturn(authHeaders);

    // when: "running the interceptor under test"
    headerInterceptor.intercept(chain);

    // then: "the expected authorization header is added to the request before proceeding"
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(chain).proceed(requestCaptor.capture());
    Request request = requestCaptor.getValue();
    assertThat(request.headers(Header.USER.getHeader())).isEqualTo(List.of("some user"));
    assertThat(request.headers(Header.ACCOUNTS.getHeader())).isEqualTo(List.of("Some ACCOUNTS"));
  }
}
