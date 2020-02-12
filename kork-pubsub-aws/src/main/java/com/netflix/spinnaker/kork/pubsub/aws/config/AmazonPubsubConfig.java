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
 */

package com.netflix.spinnaker.kork.pubsub.aws.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.kork.aws.bastion.BastionConfig;
import com.netflix.spinnaker.kork.pubsub.PubsubPublishers;
import com.netflix.spinnaker.kork.pubsub.PubsubSubscribers;
import com.netflix.spinnaker.kork.pubsub.aws.SNSPublisherProvider;
import com.netflix.spinnaker.kork.pubsub.aws.SQSSubscriberProvider;
import com.netflix.spinnaker.kork.pubsub.aws.api.AmazonPubsubMessageHandlerFactory;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnExpression("${pubsub.enabled:false} && ${pubsub.amazon.enabled:false}")
@EnableConfigurationProperties(AmazonPubsubProperties.class)
@Import(BastionConfig.class)
public class AmazonPubsubConfig {
  @Valid @Autowired private AmazonPubsubProperties amazonPubsubProperties;

  @Bean
  SQSSubscriberProvider subscriberProvider(
      AWSCredentialsProvider awsCredentialsProvider,
      AmazonPubsubProperties properties,
      PubsubSubscribers subscribers,
      AmazonPubsubMessageHandlerFactory messageHandlerFactory,
      Registry registry) {
    return new SQSSubscriberProvider(
        awsCredentialsProvider, properties, subscribers, messageHandlerFactory, registry);
  }

  @Bean
  SNSPublisherProvider publisherProvider(
      AWSCredentialsProvider awsCredentialsProvider,
      AmazonPubsubProperties properties,
      PubsubPublishers pubsubPublishers,
      Registry registry) {
    return new SNSPublisherProvider(awsCredentialsProvider, properties, pubsubPublishers, registry);
  }
}
