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

package com.netflix.spinnaker.security

import org.slf4j.MDC
import spock.lang.Specification
import spock.lang.Subject

class ThreadSecurityStoreSpec extends Specification {

  @Subject
  ThreadSecurityStore subject = new ThreadSecurityStore()

  def cleanup() {
    subject.clear()
  }

  def "should not propagate store across threads"() {
    given:
    subject.putOrRemove("test", "string")

    when:
    def childThreadValue
    def t = Thread.start {
      childThreadValue = subject.get("test")
    }
    t.join()

    then:
    subject.get("test") == "string"
    childThreadValue == null
  }

  def "should remove keys given null values"() {
    when:
    subject.putOrRemove("test", "string")

    then:
    subject.get("test") == "string"
    MDC.get("test") == "string"

    when:
    subject.putOrRemove("test", null)

    then:
    subject.get("test") == null
    !subject.getCopy().containsKey("test")
  }

  def "should return copied map"() {
    given:
    subject.putOrRemove("test", "string")

    when:
    subject.getCopy().put("test", "nope")

    then:
    subject.get("test") == "string"
  }

  def "should replace entire map on set"() {
    given:
    subject.putOrRemove("test", "string")

    when:
    subject.set([hello: "world"])

    then:
    def copy = subject.getCopy()
    copy.size() == 1
    copy["hello"] == "world"
    MDC.copyOfContextMap.size() == 1
    MDC.copyOfContextMap["hello"] == "world"
  }

  def "clear removes all entries from store"() {
    given:
    subject.putOrRemove("test", "string")

    when:
    subject.clear()

    then:
    subject.getCopy().isEmpty()
    MDC.copyOfContextMap == null
  }
}
