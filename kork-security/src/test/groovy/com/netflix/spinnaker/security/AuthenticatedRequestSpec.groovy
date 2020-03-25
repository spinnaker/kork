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

package com.netflix.spinnaker.security

import com.netflix.spinnaker.kork.common.Header
import org.slf4j.MDC
import spock.lang.Specification

class AuthenticatedRequestSpec extends Specification {
  void "should extract user details by priority (Principal > MDC)"() {
    when:
    ThreadSecurityStore.clear()
    ThreadSecurityStore.putOrRemove(Header.USER.header, "spinnaker-user")

    then:
    AuthenticatedRequest.getSpinnakerUser().get() == "spinnaker-user"
    AuthenticatedRequest.getSpinnakerUser(new User([email: "spinnaker-other"])).get() == "spinnaker-other"
  }

  void "should extract allowed account details by priority (Principal > MDC"() {
    when:
    ThreadSecurityStore.clear()
    ThreadSecurityStore.putOrRemove(Header.ACCOUNTS.header, "account1,account2")

    then:
    AuthenticatedRequest.getSpinnakerAccounts().get() == "account1,account2"
    AuthenticatedRequest.getSpinnakerAccounts(new User(allowedAccounts: ["account3", "account4"])).get() == "account3,account4"
  }

  void "should have no user/allowed account details if no MDC or Principal available"() {
    when:
    ThreadSecurityStore.clear()

    then:
    !AuthenticatedRequest.getSpinnakerUser().present
    !AuthenticatedRequest.getSpinnakerAccounts().present
  }

  void "should propagate user/allowed account details"() {
    when:
    ThreadSecurityStore.putOrRemove(Header.USER.header, "spinnaker-user")
    ThreadSecurityStore.putOrRemove(Header.ACCOUNTS.header, "account1,account2")

    def closure = AuthenticatedRequest.propagate({
      assert AuthenticatedRequest.getSpinnakerUser().get() == "spinnaker-user"
      assert AuthenticatedRequest.getSpinnakerAccounts().get() == "account1,account2"
      return true
    })

    ThreadSecurityStore.putOrRemove(Header.USER.header, "spinnaker-another-user")
    ThreadSecurityStore.putOrRemove(Header.ACCOUNTS.header, "account1,account3")
    closure.call()

    then:
    // ensure MDC context is restored
    ThreadSecurityStore.get(Header.USER.header) == "spinnaker-another-user"
    ThreadSecurityStore.get(Header.ACCOUNTS.header) == "account1,account3"

    when:
    ThreadSecurityStore.clear()

    then:
    closure.call()
    ThreadSecurityStore.clear()
  }

  void "should propagate headers"() {
    when:
    ThreadSecurityStore.clear()
    ThreadSecurityStore.putOrRemove(Header.USER.header, "spinnaker-another-user")
    ThreadSecurityStore.putOrRemove(Header.makeCustomHeader("cloudprovider"), "aws")

    then:
    Map allheaders = AuthenticatedRequest.getAuthenticationHeaders()
    allheaders == [
            'X-SPINNAKER-USER': Optional.of("spinnaker-another-user"),
            'X-SPINNAKER-ACCOUNTS': Optional.empty(),
            'X-SPINNAKER-CLOUDPROVIDER': Optional.of("aws")]
  }

  void "should not fail when no headers are set"() {
    when:
    ThreadSecurityStore.clear()

    then:
    Map allheaders = AuthenticatedRequest.getAuthenticationHeaders()
    allheaders == [
            'X-SPINNAKER-USER': Optional.empty(),
            'X-SPINNAKER-ACCOUNTS': Optional.empty()]
  }
}
