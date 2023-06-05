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

package com.netflix.spinnaker.kork.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Date;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshableIdentityTest {
  SSLContext serverContext;
  SSLContext clientContext;

  @BeforeEach
  void setUp() throws Exception {
    // start with a fresh CA cert
    var ca = CertificateIdentity.generateSelfSigned();
    var caKeyFile = Files.createTempFile("ca", ".key");
    var caCertFile = Files.createTempFile("ca", ".crt");
    ca.saveAsPEM(caKeyFile, caCertFile);
    var trustManager = TrustStores.loadTrustManager(TrustStores.loadPEM(caCertFile));

    // common cert attributes
    var notBefore = new Date();
    var notAfter = new Date(notBefore.toInstant().plus(Duration.ofHours(1)).toEpochMilli());
    var tlsKeyUsage = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement);

    // generate server keypair
    var serverKeyPair = TestCrypto.generateKeyPair();
    var serverPublicKey = serverKeyPair.getPublic();
    var serverPrivateKey = serverKeyPair.getPrivate();
    // traditionally, a TLS server certificate relied on the common name in the subject for the
    // hostname, but these days, we specify the hostname in the subject alternative names extension
    var serverName = new X500Principal("CN=localhost");
    var serverBuilder =
        new JcaX509v3CertificateBuilder(
                ca.getCertificate(),
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                serverName,
                serverPublicKey)
            .addExtension(Extension.keyUsage, true, tlsKeyUsage)
            .addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth))
            .addExtension(
                Extension.subjectAlternativeName,
                false,
                new DERSequence(
                    new ASN1Encodable[] {new GeneralName(GeneralName.dNSName, "localhost")}));

    // request CA signature and store as PEM files
    var serverIdentity =
        CertificateIdentity.fromCredentials(serverPrivateKey, ca.signCertificate(serverBuilder));
    var serverKeyFile = Files.createTempFile("server", ".key");
    var serverCertFile = Files.createTempFile("server", ".crt");
    serverIdentity.saveAsPEM(serverKeyFile, serverCertFile);
    var serverIdentitySource = X509IdentitySource.fromPEM(serverKeyFile, serverCertFile);
    var server = serverIdentitySource.refreshable(Duration.ofMinutes(15));
    serverContext = server.createSSLContext(trustManager);

    // generate client keypair
    var clientKeyPair = TestCrypto.generateKeyPair();
    var clientPublicKey = clientKeyPair.getPublic();
    var clientPrivateKey = clientKeyPair.getPrivate();
    var clientName = new X500Principal("CN=spinnaker, O=spinnaker");
    var clientBuilder =
        new JcaX509v3CertificateBuilder(
                ca.getCertificate(),
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                clientName,
                clientPublicKey)
            .addExtension(Extension.keyUsage, true, tlsKeyUsage)
            .addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth))
            .addExtension(
                Extension.subjectAlternativeName,
                false,
                new DERSequence(
                    new ASN1Encodable[] {
                      new GeneralName(GeneralName.rfc822Name, "spinnaker@localhost")
                    }));

    // request CA signature and store as PEM files
    var clientIdentity =
        CertificateIdentity.fromCredentials(clientPrivateKey, ca.signCertificate(clientBuilder));
    var clientKeyFile = Files.createTempFile("client", ".key");
    var clientCertFile = Files.createTempFile("client", ".crt");
    clientIdentity.saveAsPEM(clientKeyFile, clientCertFile);
    var clientIdentitySource = X509IdentitySource.fromPEM(clientKeyFile, clientCertFile);
    var client = clientIdentitySource.refreshable(Duration.ofMinutes(15));
    clientContext = client.createSSLContext(trustManager);
  }

  @Test
  void smokeTest() throws Exception {
    var server = HttpsServer.create();
    server.setHttpsConfigurator(new HttpsConfigurator(serverContext));

    // set up a simple echo handler
    server.createContext(
        "/echo",
        exchange -> {
          try (var request = exchange.getRequestBody();
              var response = exchange.getResponseBody()) {
            byte[] body = request.readAllBytes();
            exchange.sendResponseHeaders(200, body.length);
            response.write(body);
          }
        });
    // set up a random server port to serve from
    int port;
    try (var socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      port = socket.getLocalPort();
    }
    var serverAddress = new InetSocketAddress("localhost", port);
    server.bind(serverAddress, 0);
    server.start();
    try {
      var client = HttpClient.newBuilder().sslContext(clientContext).build();
      var request =
          HttpRequest.newBuilder(
                  new URI(
                      "https",
                      null,
                      serverAddress.getHostName(),
                      serverAddress.getPort(),
                      "/echo",
                      null,
                      null))
              .POST(HttpRequest.BodyPublishers.ofString("Hello, world!"))
              .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals("Hello, world!", response.body());
    } finally {
      server.stop(1);
    }
  }
}
