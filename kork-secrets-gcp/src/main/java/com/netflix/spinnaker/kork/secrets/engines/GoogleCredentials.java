/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.kork.secrets.engines;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.ExponentialBackOff;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

public class GoogleCredentials {

  private static final long CONNECT_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
  private static final long READ_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

  public static HttpTransport buildHttpTransport() {
    try {
      return GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException("Failed to build trusted transport", e);
    }
  }

  public static HttpRequestInitializer retryRequestInitializer() {
    return request -> {
      request.setConnectTimeout((int) CONNECT_TIMEOUT);
      request.setReadTimeout((int) READ_TIMEOUT);
      request.setUnsuccessfulResponseHandler(
          new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()));
    };
  }

  public static HttpRequestInitializer setHttpTimeout(
      final HttpRequestInitializer requestInitializer) {
    return request -> {
      requestInitializer.initialize(request);
      request.setConnectTimeout((int) CONNECT_TIMEOUT);
      request.setReadTimeout((int) READ_TIMEOUT);
      request.setUnsuccessfulResponseHandler(
          new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()));
    };
  }
}
