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

package com.netflix.spinnaker.credentials.sql

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.credentials.test.TestCredentialsDefinition
import com.netflix.spinnaker.security.Authorization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

class CredentialsDataTest {
  @Test
  fun avoidDoubleWrapping() {
    val mapper = Jackson2ObjectMapperBuilder.json().build<ObjectMapper>()
    val credentials =
      TestCredentialsDefinition("jackson", mapOf("bugs" to "features"), mapOf(Authorization.READ to setOf("johnson")))
    val json = mapper.writeValueAsString(credentials)
    val wrapped = CredentialsData(json)
    val string = mapper.writeValueAsString(wrapped)
    val unwrapped = mapper.readValue(string, TestCredentialsDefinition::class.java)
    assertEquals("jackson", unwrapped.name)
  }
}
