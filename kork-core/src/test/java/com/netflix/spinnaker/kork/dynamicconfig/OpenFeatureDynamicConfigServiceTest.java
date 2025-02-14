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

import static org.junit.jupiter.api.Assertions.*;

import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.providers.memory.Flag;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenFeatureDynamicConfigServiceTest {
  OpenFeatureDynamicConfigService service;

  @BeforeEach
  void setUp() {
    Map<String, Flag<?>> flags = new HashMap<>();
    flags.put(
        "flag.enabled",
        Flag.<Boolean>builder()
            .variant("on", true)
            .variant("off", false)
            .defaultVariant("on")
            .build());
    // TODO(jvz): https://github.com/open-feature/java-sdk/issues/501
    // this should be encoded as a Flag<Long>, but there is no Long-based API available yet
    flags.put(
        "some.number",
        Flag.builder()
            .variant("default", 10000)
            .variant("alternative", 20000)
            .defaultVariant("default")
            .build());
    flags.put(
        "config",
        Flag.<String>builder()
            .variant("alpha", "1.0")
            .variant("beta", "2.0")
            .variant("gamma", "3.0")
            .defaultVariant("beta")
            .build());
    OpenFeatureAPI api = OpenFeatureAPI.getInstance();
    api.setProviderAndWait(new InMemoryProvider(flags));
    service = new OpenFeatureDynamicConfigService(api.getClient(), DynamicConfigService.NOOP);
  }

  @Test
  void canResolveBooleanFlags() {
    assertTrue(service.isEnabled("flag", false));
    assertTrue(service.isEnabled("flag.enabled", false));
    assertFalse(service.isEnabled("flag.", false));
  }

  @Test
  void canResolveLongData() {
    assertEquals(10000L, service.getConfig(Long.class, "some.number", 42L));
  }

  @Test
  void canResolveStringData() {
    assertEquals("2.0", service.getConfig(String.class, "config", "0.0"));
  }
}
