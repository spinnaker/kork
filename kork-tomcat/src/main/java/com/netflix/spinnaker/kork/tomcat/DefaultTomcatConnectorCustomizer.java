/*
 * Copyright 2020 Netflix, Inc.
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

package com.netflix.spinnaker.kork.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.StringUtils;

class DefaultTomcatConnectorCustomizer implements TomcatConnectorCustomizer {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final TomcatConfigurationProperties tomcatConfigurationProperties;

  DefaultTomcatConnectorCustomizer(TomcatConfigurationProperties tomcatConfigurationProperties) {
    this.tomcatConfigurationProperties = tomcatConfigurationProperties;
  }

  @Override
  public void customize(Connector connector) {
    this.applySSLSettings(connector);
    this.applyRelaxedURIProperties(connector);
  }

  Ssl copySslConfigurationWithClientAuth(TomcatServletWebServerFactory tomcat) {
    Ssl ssl = new Ssl();

    BeanUtils.copyProperties(tomcat.getSsl(), ssl);
    ssl.setClientAuth(Ssl.ClientAuth.NEED);
    ssl.setCiphers(
        tomcatConfigurationProperties
            .getCipherSuites()
            .toArray(new String[tomcatConfigurationProperties.getCipherSuites().size()]));
    return ssl;
  }

  void applySSLSettings(Connector connector) {
    ProtocolHandler handler = connector.getProtocolHandler();
    if (handler instanceof AbstractHttp11JsseProtocol) {
      if (((AbstractHttp11JsseProtocol) handler).isSSLEnabled()) {
        SSLHostConfig[] sslConfigs = connector.findSslHostConfigs();
        if (sslConfigs.length != 1) {
          throw new RuntimeException(
              String.format("Ssl configs: found %d, expected 1.", sslConfigs.length));
        }
        SSLHostConfig sslHostConfig = sslConfigs[0];
        sslHostConfig.setHonorCipherOrder(true);
        sslHostConfig.setCiphers(String.join(",", tomcatConfigurationProperties.getCipherSuites()));
        sslHostConfig.setProtocols(
            String.join(",", tomcatConfigurationProperties.getTlsVersions()));
      }
    }
  }

  void applyRelaxedURIProperties(Connector connector) {
    if (StringUtils.isEmpty(tomcatConfigurationProperties.getRelaxedPathCharacters())
        && StringUtils.isEmpty(tomcatConfigurationProperties.getRelaxedQueryCharacters())) {
      return;
    }

    ProtocolHandler protocolHandler = connector.getProtocolHandler();
    if (protocolHandler instanceof AbstractHttp11Protocol) {
      if (!StringUtils.isEmpty(tomcatConfigurationProperties.getRelaxedPathCharacters())) {
        ((AbstractHttp11Protocol) protocolHandler)
            .setRelaxedPathChars(tomcatConfigurationProperties.getRelaxedPathCharacters());
      }
      if (!StringUtils.isEmpty(tomcatConfigurationProperties.getRelaxedQueryCharacters())) {
        ((AbstractHttp11Protocol) protocolHandler)
            .setRelaxedPathChars(tomcatConfigurationProperties.getRelaxedPathCharacters());
      }
    } else {
      log.warn(
          "Can't apply relaxedPath/Query config to connector of type $connector.protocolHandlerClassName");
    }
  }
}
