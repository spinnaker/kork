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

package com.netflix.spinnaker.kork.core

import spock.lang.Specification

import java.util.function.Supplier

class ConfigurableRetrySpec extends Specification {

  def retrySupport = Mock(RetrySupport)
  def subject = new ConfigurableRetry(retrySupport)

  def "should pass retry configuration to retrySupport"() {
    given:
    subject.enabled = true
    def supplier = { "foo" } as Supplier<String>

    when:
    subject.retry(supplier)

    then:
    1 * retrySupport.retry(supplier, subject.maxRetries, subject.retryBackoff, subject.exponential)
  }

  def "should only retry once when not enabled"() {
    given:
    subject.enabled = false
    def supplier = { "foo" } as Supplier<String>

    when:
    subject.retry(supplier)

    then:
    1 * retrySupport.retry(supplier, 1, subject.retryBackoff, subject.exponential)
  }

}
