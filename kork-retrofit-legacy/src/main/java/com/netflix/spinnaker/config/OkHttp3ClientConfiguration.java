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
import com.netflix.spinnaker.okhttp.OkHttp3MetricsInterceptor;
import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties;
import okhttp3.CipherSuite;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient.Builder;
import okhttp3.TlsVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static java.util.stream.Collectors.toList;
import static okhttp3.ConnectionSpec.MODERN_TLS;

@Component
public class OkHttp3ClientConfiguration {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final OkHttpClientConfigurationProperties okHttpClientConfigurationProperties;
  private final OkHttp3MetricsInterceptor okHttp3MetricsInterceptor;

  @Autowired
  public OkHttp3ClientConfiguration(
    OkHttpClientConfigurationProperties okHttpClientConfigurationProperties,
    OkHttp3MetricsInterceptor okHttp3MetricsInterceptor
  ) {
    this.okHttpClientConfigurationProperties = okHttpClientConfigurationProperties;
    this.okHttp3MetricsInterceptor = okHttp3MetricsInterceptor;
  }

  /**
   * @return OkHttpClient w/ <optional> key and trust stores
   */
  private Builder create() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
    Builder okHttpClientBuilder = new Builder()
      .connectTimeout(okHttpClientConfigurationProperties.connectTimeoutMs, TimeUnit.MILLISECONDS)
      .readTimeout(okHttpClientConfigurationProperties.readTimeoutMs, TimeUnit.MILLISECONDS)
      .retryOnConnectionFailure(okHttpClientConfigurationProperties.retryOnConnectionFailure)
      .connectionPool(new ConnectionPool(
        okHttpClientConfigurationProperties.connectionPool.maxIdleConnections,
        okHttpClientConfigurationProperties.connectionPool.keepAliveDurationMs,
        TimeUnit.MILLISECONDS))
      .addInterceptor(okHttp3MetricsInterceptor);

    if (okHttpClientConfigurationProperties.keyStore == null && okHttpClientConfigurationProperties.trustStore == null) {
      return okHttpClientBuilder;
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
    okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory());

    return applyConnectionSpecs(okHttpClientBuilder);
  }

  private Builder applyConnectionSpecs(Builder okHttpClientBuilder) {
    String[] cipherSuites = Optional
      .ofNullable(okHttpClientConfigurationProperties.cipherSuites)
      .orElse(MODERN_TLS.cipherSuites().stream().map(CipherSuite::javaName).collect(toList()))
      .toArray(new String[0]);
    String[] tlsVersions = Optional
      .ofNullable(okHttpClientConfigurationProperties.tlsVersions)
      .orElse(MODERN_TLS.tlsVersions().stream().map(TlsVersion::javaName).collect(toList()))
      .toArray(new String[0]);

    ConnectionSpec connectionSpec = new ConnectionSpec.Builder(MODERN_TLS)
      .cipherSuites(cipherSuites)
      .tlsVersions(tlsVersions)
      .build();

    return okHttpClientBuilder
      .connectionSpecs(Arrays.asList(connectionSpec, ConnectionSpec.CLEARTEXT));
  }
}
