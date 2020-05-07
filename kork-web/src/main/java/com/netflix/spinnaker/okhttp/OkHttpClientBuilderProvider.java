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

package com.netflix.spinnaker.okhttp;

import okhttp3.OkHttpClient;

public abstract class OkHttpClientBuilderProvider {

  private final OkHttpClient okHttpClient;
  private final OkHttpClientConfigurationProperties okHttpClientConfigurationProperties;

  public OkHttpClientBuilderProvider(
      OkHttpClient okHttpClient,
      OkHttpClientConfigurationProperties okHttpClientConfigurationProperties) {
    this.okHttpClient = okHttpClient;
    this.okHttpClientConfigurationProperties = okHttpClientConfigurationProperties;
  }

  /**
   * Returns whether or not the provider supports the provided url.
   *
   * @param baseUrl url
   * @return true if supports the url given
   */
  public abstract Boolean supports(String baseUrl);

  /** Allows custom implementations to adjust the url before making a call. */
  public String getNormalizedUrl(String baseUrl) {
    return baseUrl;
  }

  /**
   * Apply host name verifier for the provided url
   *
   * @param builder builder to operate on
   * @param baseUrl url
   * @return the builder.
   */
  public OkHttpClient.Builder applyHostNameVerifier(OkHttpClient.Builder builder, String baseUrl) {
    // Concrete impls will override.
    return builder;
  }

  /**
   * Creates a new OkHttpClient Builder from the client. The generated client builder is not set to
   * talk to any endpoint and it just constructs a bare minimum client
   */
  public OkHttpClient.Builder create() {
    return okHttpClient.newBuilder();
  }

  /** Creates a new OkHttpClient Builder from the client and applies custom host name verifier. */
  public OkHttpClient.Builder create(String url) {
    return applyHostNameVerifier(create(), url);
  }

  protected OkHttpClientConfigurationProperties getOkHttpClientConfigurationProperties() {
    return this.okHttpClientConfigurationProperties;
  }
}
