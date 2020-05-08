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

package com.netflix.spinnaker.config.okhttp3;

import okhttp3.OkHttpClient;

public interface OkHttpClientBuilderProvider {

  /**
   * Returns whether or not the provider supports the provided url.
   *
   * @param baseUrl url
   * @return true if supports the url given
   */
  default Boolean supports(String baseUrl) {
    return baseUrl.startsWith("http://") || baseUrl.startsWith("https://");
  }

  /** Allows custom implementations to adjust the url before making a call. */
  default String getNormalizedUrl(String baseUrl) {
    return baseUrl;
  }

  /**
   * Apply host name verifier for the provided url
   *
   * @param builder builder to operate on
   * @param baseUrl url
   * @return the builder.
   */
  default OkHttpClient.Builder setSSLSocketFactory(OkHttpClient.Builder builder, String baseUrl) {
    // Concrete impls will override.
    return builder;
  }

  /** Creates a new OkHttpClient Builder from the client and applies custom host name verifier. */
  OkHttpClient.Builder get(String url);
}
