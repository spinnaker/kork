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

package com.netflix.spinnaker.config.okhttp3;

import static java.lang.String.format;

import com.netflix.spinnaker.kork.exceptions.SystemException;
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
   * Consults the provider impls to decide which provider class can build a client for the given url
   * and delegates to that provider to build a client.
   *
   * @param endPointUrl
   * @return okHttpClient
   */
  OkHttpClient getClient(String endPointUrl) {
    OkHttpClientBuilderProvider provider = findProvider(endPointUrl);
    return provider.get(endPointUrl).build();
  }

  /**
   * Get normalized url as decided by the provider.
   *
   * @param endPointUrl
   * @return
   */
  String getNormalizedUrl(String endPointUrl) {
    return findProvider(endPointUrl).getNormalizedUrl(endPointUrl);
  }

  private OkHttpClientBuilderProvider findProvider(String endPointUrl) {
    OkHttpClientBuilderProvider providerImpl =
        providers.stream()
            .filter(provider -> provider.supports(endPointUrl))
            .findFirst()
            .orElse(null);

    if (providerImpl == null) {
      throw new SystemException(format("No client provider found for url (%s)", endPointUrl));
    }

    return providerImpl;
  }
}
