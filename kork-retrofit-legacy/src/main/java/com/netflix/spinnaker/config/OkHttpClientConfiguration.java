/*
 * Copyright 2015 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.netflix.spinnaker.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties;
import com.netflix.spinnaker.okhttp.OkHttpMetricsInterceptor;
import com.squareup.okhttp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static com.squareup.okhttp.ConnectionSpec.MODERN_TLS;
import static java.util.stream.Collectors.toList;

/**
 * @deprecated replaced by {@link OkHttp3ClientConfiguration}
 */
@Component
@Deprecated // see OkHttp3ClientConfiguration
public class OkHttpClientConfiguration {

  private final OkHttpClientConfigurationProperties okHttpClientConfigurationProperties;
  private final OkHttpMetricsInterceptor okHttpMetricsInterceptor;

  @Autowired
  public OkHttpClientConfiguration(
    OkHttpClientConfigurationProperties okHttpClientConfigurationProperties,
    OkHttpMetricsInterceptor okHttpMetricsInterceptor
  ) {
    this.okHttpClientConfigurationProperties = okHttpClientConfigurationProperties;
    this.okHttpMetricsInterceptor = okHttpMetricsInterceptor;
  }

  /**
   * @return OkHttpClient w/ <optional> key and trust stores
   */
  public OkHttpClient create() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
    OkHttpClient okHttpClient = new OkHttpClient();
    okHttpClient.setConnectTimeout(okHttpClientConfigurationProperties.connectTimeoutMs, TimeUnit.MILLISECONDS);
    okHttpClient.setReadTimeout(okHttpClientConfigurationProperties.readTimeoutMs, TimeUnit.MILLISECONDS);
    okHttpClient.setRetryOnConnectionFailure(okHttpClientConfigurationProperties.retryOnConnectionFailure);
    okHttpClient.interceptors().add(okHttpMetricsInterceptor);
    okHttpClient.setConnectionPool(new ConnectionPool(
      okHttpClientConfigurationProperties.connectionPool.maxIdleConnections,
      okHttpClientConfigurationProperties.connectionPool.keepAliveDurationMs
    ));

    if (okHttpClientConfigurationProperties.keyStore == null && okHttpClientConfigurationProperties.trustStore == null) {
      return okHttpClient;
    }

    SSLContext sslContext = SSLContext.getInstance("TLS");

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    KeyStore ks = KeyStore.getInstance(okHttpClientConfigurationProperties.keyStoreType);
    try (InputStream it = new FileInputStream(okHttpClientConfigurationProperties.keyStore)) {
      ks.load(it, okHttpClientConfigurationProperties.keyStorePassword.toCharArray());
    }
    keyManagerFactory.init(ks, okHttpClientConfigurationProperties.keyStorePassword.toCharArray());

    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyStore ts = KeyStore.getInstance(okHttpClientConfigurationProperties.trustStoreType);
    try (InputStream it = new FileInputStream(okHttpClientConfigurationProperties.trustStore)) {
      ts.load(it, okHttpClientConfigurationProperties.trustStorePassword.toCharArray());
    }
    trustManagerFactory.init(ts);

    SecureRandom secureRandom = new SecureRandom();
    try {
      secureRandom = SecureRandom.getInstance(okHttpClientConfigurationProperties.secureRandomInstanceType);
    } catch (NoSuchAlgorithmException e) {
      log.error("Unable to fetch secure random instance for {}", okHttpClientConfigurationProperties.secureRandomInstanceType, e);
    }

    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), secureRandom);
    okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());

    return applyConnectionSpecs(okHttpClient);
  }

  private OkHttpClient applyConnectionSpecs(OkHttpClient okHttpClient) {
    String[] cipherSuites = Optional.ofNullable(okHttpClientConfigurationProperties.cipherSuites)
      .orElse(MODERN_TLS.cipherSuites().stream().map(CipherSuite::name).collect(toList()))
      .toArray(new String[0]);
    String[] tlsVersions = Optional.ofNullable(okHttpClientConfigurationProperties.tlsVersions)
      .orElse(MODERN_TLS.tlsVersions().stream().map(TlsVersion::javaName).collect(toList()))
      .toArray(new String[0]);

    ConnectionSpec connectionSpec = new ConnectionSpec.Builder(MODERN_TLS)
      .cipherSuites(cipherSuites)
      .tlsVersions(tlsVersions)
      .build();

    return okHttpClient.setConnectionSpecs(Arrays.asList(connectionSpec, ConnectionSpec.CLEARTEXT));
  }

  private final Logger log = LoggerFactory.getLogger(getClass());
}
