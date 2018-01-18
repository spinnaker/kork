/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.retrofit;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import retrofit.Profiler;

public class RetrofitSpectatorProfiler implements Profiler<Object> {

  private final String serviceName;
  private final Registry registry;

  public RetrofitSpectatorProfiler(String serviceName, Registry registry) {
    this.serviceName = serviceName;
    this.registry = registry;
  }

  @Override
  public Object beforeCall() {
    return null;
  }

  @Override
  public void afterCall(RequestInformation requestInfo, long elapsedTime, int statusCode, Object beforeCallData) {
    Id id = registry.createId(String.format("retrofit.%s.requestTimeMs", serviceName))
      .withTag("method", requestInfo.getMethod())
      .withTag("endpoint", requestInfo.getRelativePath())
      .withTag("status", Integer.toString(statusCode));
    registry.gauge(id, elapsedTime);
  }
}
