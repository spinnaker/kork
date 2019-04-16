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

package com.netflix.spinnaker.config

import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties
import com.netflix.spinnaker.tomcat.TomcatContainerCustomizerUtil
import com.netflix.spinnaker.tomcat.x509.SslExtensionConfigurationProperties
import groovy.util.logging.Slf4j
import org.apache.catalina.connector.Connector
import org.apache.coyote.http11.Http11NioProtocol
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Slf4j
@Configuration
@ComponentScan(basePackages = ["com.netflix.spinnaker.endpoint"])
@EnableConfigurationProperties([
  ResolvedEnvironmentConfigurationProperties,
  SslExtensionConfigurationProperties,
  TomcatConfigurationProperties
])
class TomcatConfiguration {

  @Bean
  TomcatContainerCustomizerUtil tomcatContainerCustomizerUtil(OkHttpClientConfigurationProperties okHttpClientConfigurationProperties,
                                                              SslExtensionConfigurationProperties sslExtensionConfigurationProperties) {
    return new TomcatContainerCustomizerUtil(okHttpClientConfigurationProperties, sslExtensionConfigurationProperties)
  }

  /**
   * Setup multiple connectors:
   * - an https connector requiring client auth that will service API requests
   * - an http connector that will service legacy non-https requests
   */
  @Bean
  @ConditionalOnExpression('${server.ssl.enabled:false}')
  WebServerFactoryCustomizer containerCustomizer(TomcatContainerCustomizerUtil tomcatContainerCustomizerUtil,
                                                 TomcatConfigurationProperties tomcatConfigurationProperties) throws Exception {
    System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true")
    System.setProperty("jdk.tls.ephemeralDHKeySize", "2048")

    return { factory ->
      TomcatServletWebServerFactory tomcat = (TomcatServletWebServerFactory) factory

      // This will only handle the case where SSL is enabled on the main Tomcat connector
      tomcat.addConnectorCustomizers(new TomcatConnectorCustomizer() {
        @Override
        void customize(Connector connector) {
          tomcatContainerCustomizerUtil.applySSLSettings(connector)
        }
      })

      if (tomcatConfigurationProperties.legacyServerPort > 0) {
        log.info("Creating legacy connecgtor on port ${tomcatConfigurationProperties.legacyServerPort}")
        def httpConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol")
        httpConnector.scheme = "http"
        httpConnector.port = tomcatConfigurationProperties.legacyServerPort
        tomcat.addAdditionalTomcatConnectors(httpConnector)
      }

      if (tomcatConfigurationProperties.apiPort > 0) {
        log.info("Creating api connector on port ${tomcatConfigurationProperties.apiPort}")
        def apiConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol")
        apiConnector.scheme = "https"
        apiConnector.port = tomcatConfigurationProperties.apiPort

        def ssl = tomcatContainerCustomizerUtil.copySslConfigurationWithClientAuth(tomcat)
        Http11NioProtocol handler = apiConnector.getProtocolHandler() as Http11NioProtocol
        // TODO(rz): no more:
        tomcat.configureSsl(handler, ssl)
        tomcatContainerCustomizerUtil.applySSLSettings(apiConnector)
        tomcat.addAdditionalTomcatConnectors(apiConnector)
      }
    } as WebServerFactoryCustomizer
  }
}
