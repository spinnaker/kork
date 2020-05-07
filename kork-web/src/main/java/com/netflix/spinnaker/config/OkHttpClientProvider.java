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

import static java.lang.String.format;

import com.netflix.spinnaker.kork.exceptions.SystemException;
import com.netflix.spinnaker.okhttp.OkHttpClientBuilderProvider;
import java.util.List;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
class OkHttpClientProvider {

  private final List<OkHttpClientBuilderProvider> providers;

  public OkHttpClientProvider(List<OkHttpClientBuilderProvider> providers) {
    this.providers = providers;
  }

  /**
   * consults the provider impls to decide which provider class can build a client for the given url
   * and delegates to that provider to build a client.
   *
   * @param endPointUrl
   * @return okHttpClient
   */
  OkHttpClient getClient(String endPointUrl) {

    OkHttpClientBuilderProvider provider = findProvider(endPointUrl);
    if (provider == null) {
      throw new SystemException(format("Failed to create HTTP client for url (%s)", endPointUrl));
    }

    OkHttpClient.Builder builder = provider.create();
    provider.applyHostNameVerifier(builder, endPointUrl);

    return builder.build();
  }

  /**
   * Get normalized URL as decided by the provider that can serve the presented Url.
   *
   * @param endPointUrl
   * @return
   */
  String getNormalizedUrl(String endPointUrl) {
    OkHttpClientBuilderProvider provider = findProvider(endPointUrl);
    return (provider != null) ? provider.getNormalizedUrl(endPointUrl) : null;
  }

  private OkHttpClientBuilderProvider findProvider(String endPointUrl) {
    return providers.stream()
        .filter(provider -> provider.supports(endPointUrl))
        .findFirst()
        .orElse(null);
  }
}
