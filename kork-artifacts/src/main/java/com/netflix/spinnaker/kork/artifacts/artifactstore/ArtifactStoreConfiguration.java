/*
 * Copyright 2023 Apple Inc.
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
package com.netflix.spinnaker.kork.artifacts.artifactstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.artifacts.artifactstore.s3.S3ArtifactStoreGetter;
import com.netflix.spinnaker.kork.artifacts.artifactstore.s3.S3ArtifactStoreStorer;
import java.net.URI;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@ConditionalOnExpression("${artifact-store.enabled:false}")
@EnableConfigurationProperties(ArtifactStoreConfigurationProperties.class)
@Log4j2
public class ArtifactStoreConfiguration {
  /**
   * this is strictly used due to Spring and Jackson not behaving nicely together.
   * Unfortunately, @JsonDeserializer will construct its own deserializer utilizing beans and thus
   * not using the object mapper we want to use
   */
  @Bean
  public ObjectMapper artifactObjectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public ArtifactStoreURIBuilder artifactStoreURIBuilder() {
    return new ArtifactStoreURISHA256Builder();
  }

  @Bean(destroyMethod = "clearInstance")
  public ArtifactStore artifactStore(
      ArtifactStoreGetter artifactStoreGetter, ArtifactStoreStorer artifactStoreStorer) {

    ArtifactStore storage = new ArtifactStore(artifactStoreGetter, artifactStoreStorer);
    ArtifactStore.setInstance(storage);
    return storage;
  }

  @Bean
  @ConditionalOnProperty(
      value = "artifact-store.s3.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public NoopArtifactStoreStorer noopArtifactStoreStorer() {
    return new NoopArtifactStoreStorer();
  }

  @Bean
  @ConditionalOnExpression("${artifact-store.s3.enabled:false}")
  public S3ArtifactStoreStorer s3ArtifactStoreStorer(
      ArtifactStoreConfigurationProperties properties,
      @Qualifier("artifactS3Client") S3Client s3Client,
      ArtifactStoreURIBuilder artifactStoreURIBuilder) {
    return new S3ArtifactStoreStorer(
        s3Client,
        properties.getS3().getBucket(),
        artifactStoreURIBuilder,
        properties.getApplicationsRegex());
  }

  @Bean
  public S3ArtifactStoreGetter s3ArtifactStoreGetter(
      Optional<PermissionEvaluator> permissionEvaluator,
      ArtifactStoreConfigurationProperties properties,
      @Qualifier("artifactS3Client") S3Client s3Client) {

    if (permissionEvaluator.isEmpty()) {
      log.warn(
          "PermissionEvaluator is not present. This means anyone will be able to access any artifact in the store.");
    }

    return new S3ArtifactStoreGetter(
        s3Client, permissionEvaluator.orElse(null), properties.getS3().getBucket());
  }

  @Bean
  public S3Client artifactS3Client(ArtifactStoreConfigurationProperties properties) {
    S3ClientBuilder builder = S3Client.builder();
    ArtifactStoreConfigurationProperties.S3ClientConfig config = properties.getS3();

    // Overwriting the URL is primarily used for S3 compatible object stores
    // like seaweedfs
    if (config.getUrl() != null) {
      builder =
          builder
              .credentialsProvider(getCredentialsProvider(config))
              .forcePathStyle(config.isForcePathStyle())
              .endpointOverride(URI.create(config.getUrl()));
    } else if (config.getProfile() != null) {
      builder = builder.credentialsProvider(ProfileCredentialsProvider.create(config.getProfile()));
    }

    if (config.getRegion() != null) {
      builder = builder.region(Region.of(config.getRegion()));
    }

    return builder.build();
  }

  private AwsCredentialsProvider getCredentialsProvider(
      ArtifactStoreConfigurationProperties.S3ClientConfig config) {
    if (config.getAccessKey() != null) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey()));
    } else {
      return AnonymousCredentialsProvider.create();
    }
  }
}
