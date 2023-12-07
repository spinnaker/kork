/*
 * Copyright 2023 Apple Inc.
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

package com.netflix.spinnaker.credentials.validator;

import java.util.Map;
import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("credentials.validator")
public class CredentialsDefinitionValidatorProperties {
  private static final Pattern DEFAULT_VALID_ACCOUNT_NAME_PATTERN =
      Pattern.compile("[a-zA-Z][-_a-zA-Z0-9]+[a-zA-Z0-9]");

  /**
   * Specifies the pattern to validate allowed account names by account type. Account types without
   * a pattern defined here will use the default validation pattern that checks for a letter at the
   * beginning, an alphanumeric character at the end, and alphanumeric characters, hyphens, and
   * underscores in-between.
   */
  private Map<String, Pattern> validAccountNamePatterns = Map.of();

  public Pattern getValidAccountNamePattern(String accountType) {
    return validAccountNamePatterns.getOrDefault(accountType, DEFAULT_VALID_ACCOUNT_NAME_PATTERN);
  }
}
