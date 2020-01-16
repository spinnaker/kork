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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Component
@Configuration
@ConditionalOnExpression("${secrets.s3.enabled:false}")
@Slf4j
public class S3Configuration {

  private String s3Url;
  private boolean pathStyleAccessEnabled;

  @Autowired
  public S3Configuration(
      @Value("${secrets.s3.endpointUrl:}") String s3Url,
      @Value("${secrets.s3.pathStyleAccessEnabled:}") String pathStyleAccessEnabled) {
    this.s3Url = s3Url;
    this.pathStyleAccessEnabled = Boolean.parseBoolean(pathStyleAccessEnabled);
  }
}
