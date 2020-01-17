/*
 * Copyright 2020 Armory, Inc.
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

package com.netflix.spinnaker.kork.secrets.engines;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Component
@Configuration
@ConditionalOnProperty("secrets.s3.enabled")
@Slf4j
public class S3Configuration {

  private String endpointUrl;
  private boolean pathStyleAccessEnabled;

  @Autowired
  public S3Configuration(
      @Value("${secrets.s3.endpoint-url}") String endpointUrl,
      @Value("${secrets.s3.path-style-access-enabled}") boolean pathStyleAccessEnabled) {
    this.endpointUrl = endpointUrl;
    this.pathStyleAccessEnabled = pathStyleAccessEnabled;
  }
}
