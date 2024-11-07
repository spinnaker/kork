/*
 * Copyright 2024 Apple, Inc.
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

package com.netflix.spinnaker.kork.dynamicconfig;

import com.netflix.spinnaker.kork.flags.EnableOpenFeature;
import dev.openfeature.sdk.OpenFeatureAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Configuration to enable OpenFeature and set up a {@link DynamicConfigService} bean based on it.
 */
@Component
@EnableOpenFeature
public class OpenFeatureConfigConfiguration {
  @Bean
  public OpenFeatureDynamicConfigService openFeatureDynamicConfigService(
      OpenFeatureAPI api, Environment environment) {
    var fallback = new SpringDynamicConfigService();
    fallback.setEnvironment(environment);
    return new OpenFeatureDynamicConfigService(api.getClient(), fallback);
  }
}
