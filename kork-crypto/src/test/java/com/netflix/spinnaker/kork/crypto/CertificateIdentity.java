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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import javax.security.auth.x500.X500PrivateCredential;
import lombok.experimental.Delegate;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// see https://stackoverflow.com/a/44981616 for example
public class CertificateIdentity {
  public static final X500Name ISSUER = new X500Name("CN=Test Certificate Authority");

  @Delegate private final X500PrivateCredential credential;

  private CertificateIdentity(X500PrivateCredential credential) {
    this.credential = credential;
  }

  public void saveAsPKCS12(Path keystoreFile, char[] password) throws IOException {
    var entry =
        new KeyStore.PrivateKeyEntry(getPrivateKey(), new X509Certificate[] {getCertificate()});
    var protectionParameter = new KeyStore.PasswordProtection(password);
    var keyStore = StandardCrypto.getPKCS12KeyStore();
    try (var output = Files.newOutputStream(keystoreFile)) {
      keyStore.load(null, null);
      keyStore.setEntry(getAlias(), entry, protectionParameter);
      keyStore.store(output, password);
    } catch (GeneralSecurityException e) {
      throw new IOException(e);
    }
  }

  public void saveAsPEM(Path keyFile, Path certificateFile) throws IOException {
    Base64.Encoder encoder = Base64.getEncoder();
    try (var writer = Files.newBufferedWriter(keyFile)) {
      writer.write("-----BEGIN PRIVATE KEY-----");
      writer.newLine();
      writer.write(encoder.encodeToString(getPrivateKey().getEncoded()));
      writer.newLine();
      writer.write("-----END PRIVATE KEY-----");
      writer.newLine();
    }
    try (var writer = Files.newBufferedWriter(certificateFile)) {
      writer.write("-----BEGIN CERTIFICATE-----");
      writer.newLine();
      writer.write(encoder.encodeToString(getCertificate().getEncoded()));
      writer.newLine();
      writer.write("-----END CERTIFICATE-----");
      writer.newLine();
    } catch (CertificateEncodingException e) {
      throw new IOException(e);
    }
  }

  public X509Certificate signCertificate(X509v3CertificateBuilder builder)
      throws CertificateException, IOException, OperatorCreationException {
    X509CertificateHolder certificateHolder =
        builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(getPrivateKey()));
    return (X509Certificate)
        StandardCrypto.getX509CertificateFactory()
            .generateCertificate(new ByteArrayInputStream(certificateHolder.getEncoded()));
  }

  public static CertificateIdentity generateSelfSigned()
      throws IOException, GeneralSecurityException, OperatorCreationException {
    var keyPair = TestCrypto.generateKeyPair();
    var privateKey = keyPair.getPrivate();
    var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);

    // set up a certificate authority
    X509CertificateHolder certificateHolder =
        builderForPublicKey(keyPair.getPublic())
            // specify key usage to allow certificate signing as a CA cert
            .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign))
            // specify this certificate is a CA cert (a leaf certificate)
            .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
            .build(signer);
    // convert BC X.509 certificate into a standard Java X509Certificate
    CertificateFactory certificateFactory = StandardCrypto.getX509CertificateFactory();
    var certificate =
        (X509Certificate)
            certificateFactory.generateCertificate(
                new ByteArrayInputStream(certificateHolder.getEncoded()));
    return fromCredentials(privateKey, certificate);
  }

  public static CertificateIdentity fromCredentials(
      PrivateKey privateKey, X509Certificate certificate) {
    var alias = X509Identity.generateAlias(certificate);
    var credential = new X500PrivateCredential(certificate, privateKey, alias);
    return new CertificateIdentity(credential);
  }

  private static X509v3CertificateBuilder builderForPublicKey(PublicKey publicKey) {
    // ensure we use somewhat unique serial numbers
    BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
    // use a validity range of now to one year from now
    Date notBefore = new Date();
    Date notAfter = new Date(notBefore.toInstant().plus(Duration.ofDays(365)).toEpochMilli());
    return new JcaX509v3CertificateBuilder(ISSUER, serial, notBefore, notAfter, ISSUER, publicKey);
  }
}
