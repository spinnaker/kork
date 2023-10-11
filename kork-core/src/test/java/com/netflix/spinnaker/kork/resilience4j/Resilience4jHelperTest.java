/*
 * Copyright 2023 Salesforce, Inc.
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

package com.netflix.spinnaker.kork.resilience4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import com.netflix.spinnaker.kork.test.log.MemoryAppender;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Resilience4jHelperTest {

  private static final Logger log = LoggerFactory.getLogger(Resilience4jHelperTest.class);

  @Test
  void testConfigureLogging() {
    // Verify that retries configured with Resilience4jHelper.configureLogging
    // log the expected messages.

    // given:
    MemoryAppender memoryAppender = new MemoryAppender(Resilience4jHelperTest.class);

    RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    String description = "test retry description";
    Resilience4jHelper.configureLogging(retryRegistry, description, log);

    String retryName = "test retry name";
    Retry retry = retryRegistry.retry(retryName);

    RuntimeException exception = new RuntimeException("retryable exception");

    // when:
    assertThatThrownBy(
            () ->
                retry.executeRunnable(
                    () -> {
                      throw exception;
                    }))
        .isEqualTo(exception);

    // then:
    assertThat(
            memoryAppender.search(
                description + " retries configured for: " + retryName, Level.INFO))
        .hasSize(1);
    assertThat(
            memoryAppender.search(
                "Retrying "
                    + description
                    + " for "
                    + retryName
                    + ". Attempt #1 failed with exception: "
                    + exception.toString(),
                Level.INFO))
        .hasSize(1);
    assertThat(
            memoryAppender.search(
                "Retrying "
                    + description
                    + " for "
                    + retryName
                    + ". Attempt #2 failed with exception: "
                    + exception.toString(),
                Level.INFO))
        .hasSize(1);
    assertThat(
            memoryAppender.search(
                description
                    + " for "
                    + retryName
                    + " failed after 3 attempts. Exception: "
                    + exception.toString(),
                Level.ERROR))
        .hasSize(1);
  }
}
