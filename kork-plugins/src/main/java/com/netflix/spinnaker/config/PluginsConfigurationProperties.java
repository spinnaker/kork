/*
 * Copyright 2019 Netflix, Inc.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root-level configuration properties for plugins.
 *
 * @see PluginsAutoConfiguration
 */
@ConfigurationProperties("spinnaker.plugins")
public class PluginsConfigurationProperties {

  /**
   * The root filepath to the directory containing all plugins.
   *
   * <p>If an absolute path is not provided, the path will be calculated relative to the executable.
   */
  public String rootPath = "plugins";
}
