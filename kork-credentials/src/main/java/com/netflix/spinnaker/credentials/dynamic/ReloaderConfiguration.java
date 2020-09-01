/*
 * Copyright 2020 Armory
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

package com.netflix.spinnaker.credentials.dynamic;

import com.netflix.spinnaker.credentials.definition.AbstractCredentialsLoader;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("credentials.reload.enabled")
@RequiredArgsConstructor
@Slf4j
public class ReloaderConfiguration {
  private static final String DEFAULT_RELOAD_KEY = "default";
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private final ReloaderConfigurationProperties config;
  private final List<AbstractCredentialsLoader<?>> loaders;

  @PostConstruct
  public void setup() {
    loaders.stream().forEach(this::schedule);
  }

  private void schedule(AbstractCredentialsLoader<?> loader) {
    ReloaderConfigurationProperties.Settings settings =
        getReloadSettings(loader.getCredentialsRepository().getType());
    if (settings != null && settings.getReloadFrequencyMs() > 0) {
      executor.schedule(
          new Reloader<>(loader), settings.getReloadFrequencyMs(), TimeUnit.MILLISECONDS);
    }
  }

  private ReloaderConfigurationProperties.Settings getReloadSettings(String type) {
    ReloaderConfigurationProperties.Settings settings = config.getRepositories().get(type);
    if (settings == null) {
      return config.getRepositories().get(DEFAULT_RELOAD_KEY);
    }
    return settings;
  }
}
