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

package com.netflix.spinnaker.kork.pubsub.aws.api;

import com.amazonaws.services.sqs.model.Message;
import com.netflix.spinnaker.kork.pubsub.aws.SQSSubscriber;

/**
 * Each SQSSubscriber will be associated with a single AmazonPubsubMessageHandler
 *
 * @see AmazonPubsubMessageHandlerFactory
 * @see SQSSubscriber
 */
public interface AmazonPubsubMessageHandler {
  /**
   * the callback that the relevant SQSSubscriber will use when it reads a message from its queue
   * implementations can throw exceptions out of handleMessage, they will be handled by the
   * SQSSubscriber
   *
   * @param message the raw SQS message read from the queue
   */
  void handleMessage(Message message);
}
