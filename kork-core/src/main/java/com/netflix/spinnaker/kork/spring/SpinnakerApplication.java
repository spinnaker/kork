package com.netflix.spinnaker.kork.spring;
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

import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class SpinnakerApplication extends SpringBootServletInitializer {

  public static void initialize(Map<String, Object> default_props, Class source, String... args) {
    PluginLoader.addPlugins();
    PluginLoader.getEnabledJarPaths();
    new SpringApplicationBuilder().properties(default_props).sources(source).run(args);
  }
}
