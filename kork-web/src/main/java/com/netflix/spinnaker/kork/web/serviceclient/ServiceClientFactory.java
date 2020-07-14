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

import com.netflix.spinnaker.config.ServiceEndpoint;

/** Factory to build clients/builders that help to make remote http calls. */
public interface ServiceClientFactory {

  /**
   * Builds a concrete client capable of making HTTP calls.
   *
   * @param type client type
   * @param serviceEndpoint endpoint configuration
   * @param <T> type of client , usually a interface with all the remote method definitions.
   * @return a implementation of the type of client given.
   */
  public <T> T getClient(Class<T> type, ServiceEndpoint serviceEndpoint);

  /**
   * Decide if this factory can support the endpoint provided.
   *
   * @param type client type
   * @param serviceEndpoint endpoint configuration
   * @param <T> type of client , usually represents a interface with all the remote method
   *     definitions.
   * @return a client builder
   */
  public default <T> boolean supports(Class<T> type, ServiceEndpoint serviceEndpoint) {
    return true;
  }
}
