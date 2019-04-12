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

package com.netflix.spinnaker.config.secrets;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;


/**
 * EncryptedSecrets contain an engineIdentifier and named parameters. EncryptedSecrets can
 * be used by a SecretEngine implementation to get the decrypted secret.
 *
 * Format for Encrypted Secrets:
 *
 * encrypted:&lt;engine-identifier&gt;!&lt;param-name_1&gt;:&lt;param-value_1&gt;!..!&lt;param-name_n&gt;:&lt;param-value_n&gt;
 *
 * Note: Valid param-names match the regex: `[a-zA-Z0-9]+`
 * Note: secret-params may contain ':'
 * Note: `encrypted` cannot be a param-name
 * Note: There must be at least one &lt;param-name&gt;:&lt;param-value&gt; pair
 * Named parameters are used to allow for adding additional options in the future.
 */

@EqualsAndHashCode
@NoArgsConstructor
public class EncryptedSecret {

  public static final String ENCRYPTED_STRING_PREFIX = "encrypted:";
  private static final String ENCRYPTED_STRING_REGEX = ENCRYPTED_STRING_PREFIX + ".+(![a-zA-Z0-9]+:.+)+";

  @Getter @Setter
  private String engineIdentifier;

  @Getter
  private Map<String, String> params = new HashMap<>();

  EncryptedSecret(String secretConfig) {
    this.update(secretConfig);
  }

  /**
   * @param secretConfig
   * @return EncryptedSecret object
   */
  public static EncryptedSecret parse(String secretConfig) {
    if (EncryptedSecret.isEncryptedSecret(secretConfig)) {
      return new EncryptedSecret(secretConfig);
    }
    return null;
  }

  protected void update(String secretConfig) {
    String[] keyValues = secretConfig.split("!");
    if (keyValues.length < 2) {
      throw new InvalidSecretFormatException("Invalid encrypted secret format, must have at least one parameter");
    }
    for (int i=0; i < keyValues.length; i++) {
      String[] keyV = keyValues[i].split(":", 2);
      if (keyV.length != 2) {
        throw new InvalidSecretFormatException("Invalid encrypted secret format, keys and values must be delimited by ':'");
      }
      if (i == 0) {
        this.engineIdentifier = keyV[1];
      } else {
        this.params.put(keyV[0], keyV[1]);
      }
    }
  }

  /**
   * @param secretConfig
   * @return boolean representing whether or not the secretConfig is formatted correctly
   */
  public static boolean isEncryptedSecret(String secretConfig) {
    return secretConfig != null && secretConfig.startsWith(ENCRYPTED_STRING_PREFIX) && secretConfig.matches(ENCRYPTED_STRING_REGEX);
  }

  /**
   * @return formatted encrypted secret
   */
  public String formatString() {
    StringBuilder sb = new StringBuilder(ENCRYPTED_STRING_PREFIX)
      .append(engineIdentifier);
    for (String key : params.keySet()) {
      sb.append('!')
        .append(key)
        .append(':')
        .append(params.get(key));
    }
    return sb.toString();
  }

}
