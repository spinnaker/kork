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

package com.netflix.spinnaker.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Represents a service endpoint URL and name. */
public class DefaultServiceEndpoint implements ServiceEndpoint {

  /** Name of the service */
  @Nonnull private final String name;

  /** Base API url */
  @Nonnull private final String baseUrl;

  /** Indicates whether the certificate/host verification is desired or not */
  @Nonnull private final Boolean isSecure;

  /** Misc. config necessary for the service client. */
  @Nonnull private final Map<String, Object> config;

  public DefaultServiceEndpoint(@Nonnull String name, @Nonnull String baseUrl) {
    this.name = Objects.requireNonNull(name);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.isSecure = true;
    this.config = new HashMap<>();
  }

  public DefaultServiceEndpoint(
      @Nonnull String name, @Nonnull String baseUrl, @Nonnull Boolean isSecure) {
    this.name = Objects.requireNonNull(name);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.isSecure = Objects.requireNonNull(isSecure);
    this.config = new HashMap<>();
  }

  public DefaultServiceEndpoint(
      @Nonnull String name, @Nonnull String baseUrl, @Nonnull Map<String, Object> config) {
    this.name = Objects.requireNonNull(name);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.config = Objects.requireNonNull(config);
    this.isSecure = true;
  }

  public DefaultServiceEndpoint(
      @Nonnull String name,
      @Nonnull String baseUrl,
      @Nonnull Map<String, Object> config,
      @Nonnull Boolean isSecure) {
    this.name = Objects.requireNonNull(name);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.config = Objects.requireNonNull(config);
    this.isSecure = isSecure;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  @Override
  @Nonnull
  public String getBaseUrl() {
    return baseUrl;
  }

  @Nonnull
  @Override
  public Map<String, Object> getConfig() {
    return config;
  }

  @Override
  public boolean isSecure() {
    return isSecure;
  }
}
