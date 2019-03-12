/*
 * Copyright 2019 Netflix, Inc.
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
package com.netflix.spinnaker.kork.refactor;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.isEmpty;

@Slf4j
@Component
public class DeprecationRegistry {

  private final Registry registry;

  public DeprecationRegistry(Registry registry) {
    this.registry = registry;
  }

  public void logDeprecationUsage(final String deprecationName, Map<String, String> tags) {
    if (isEmpty(deprecationName)) {
      log.warn("No deprecation name ({}) provided - ignoring publish of deprecated usage", deprecationName);
      return;
    }

    Map<String, String> prunedTags = tags.entrySet().stream()
      .filter(e -> {
        if (isEmpty(e.getKey()) || isEmpty(e.getValue())) {
          log.warn(
            "Deprecation tag key ({}) or value ({}) is empty for deprecation ({}) - ignoring tag",
            e.getKey(),
            e.getValue(),
            deprecationName
          );
          return false;
        }
        return true;
      }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Id id = registry.createId(format("deprecation.%s", deprecationName))
      .withTags(prunedTags);
    registry.counter(id).increment();
  }
}
