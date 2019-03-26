/*
 * Copyright 2016 Netflix, Inc.
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

package com.netflix.spinnaker.okhttp;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix="ok-http-client")
class OkHttpClientConfigurationProperties {
  long connectTimeoutMs = 15000;
  long readTimeoutMs = 20000;

  public boolean propagateSpinnakerHeaders = true;

  public File keyStore;
  public String keyStoreType = "PKCS12";
  public String keyStorePassword = "changeit";

  public File trustStore;
  public String trustStoreType = "PKCS12";
  public String trustStorePassword = "changeit";

  public String secureRandomInstanceType = "NativePRNGNonBlocking";

  public List<String> tlsVersions = Arrays.asList("TLSv1.2", "TLSv1.1");
  //Defaults from https://wiki.mozilla.org/Security/Server_Side_TLS#Modern_compatibility
  // with some extra ciphers (non SHA384/256) to support TLSv1.1 and some non EC ciphers
  public List<String> cipherSuites = Arrays.asList(
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA"
  );

  /**
   * Provide backwards compatibility for 'okHttpClient.connectTimoutMs'
   */
  public void setConnectTimoutMs(long connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
  }

  public static class ConnectionPoolProperties {
    public int maxIdleConnections = 5;
    public int keepAliveDurationMs = 30000;
  }

  @NestedConfigurationProperty
  public final ConnectionPoolProperties connectionPool = new ConnectionPoolProperties();

  public boolean retryOnConnectionFailure = true;
}
