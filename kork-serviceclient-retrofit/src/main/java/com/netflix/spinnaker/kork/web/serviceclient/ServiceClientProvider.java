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

import static java.lang.String.format;

import com.netflix.spinnaker.config.ServiceEndpoint;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.exceptions.SystemException;
import java.util.List;

/** Provider that returns a suitable service client capable of making http calls. */
@NonnullByDefault
public class ServiceClientProvider {

  private final List<ServiceClientFactory> serviceClientFactories;

  ServiceClientProvider(List<ServiceClientFactory> serviceClientFactories) {
    this.serviceClientFactories = serviceClientFactories;
  }

  public <T> T getClient(Class<T> type, ServiceEndpoint serviceEndpoint) {
    ServiceClientFactory serviceClientFactory = findProvider(type, serviceEndpoint);
    return serviceClientFactory.getClient(type, serviceEndpoint);
  }

  private ServiceClientFactory findProvider(Class<?> type, ServiceEndpoint service) {
    return serviceClientFactories.stream()
        .filter(provider -> provider.supports(type, service))
        .findFirst()
        .orElseThrow(
            () ->
                new SystemException(
                    format("No service client provider found for url (%s)", service.getBaseUrl())));
  }
}
