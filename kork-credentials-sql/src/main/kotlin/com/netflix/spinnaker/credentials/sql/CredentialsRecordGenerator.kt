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

package com.netflix.spinnaker.credentials.sql

import com.fasterxml.jackson.core.JsonProcessingException
import com.netflix.spinnaker.credentials.CredentialsTypes
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionMapper
import com.netflix.spinnaker.kork.web.exceptions.InvalidRequestException
import io.micrometer.core.instrument.Clock
import org.jooq.JSON
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CredentialsRecordGenerator(
  private val mapper: CredentialsDefinitionMapper,
  private val clock: Clock
) {
  fun create(definition: CredentialsDefinition): CredentialsRecord =
    create(definition, clock.wallTime(), whoami())

  fun create(definitions: Collection<CredentialsDefinition>): List<CredentialsRecord> {
    val now = clock.wallTime()
    val user = whoami()
    return definitions.map { create(it, now, user) }
  }

  private fun create(definition: CredentialsDefinition, now: Long, user: String): CredentialsRecord {
    val definitionClass = definition.javaClass
    val typeName = CredentialsTypes.getCredentialsTypeName(definitionClass)
      ?: throw InvalidRequestException("No @CredentialsType or @JsonTypeName defined for $definitionClass")
    val name = definition.name
    val json = try {
      JSON.valueOf(mapper.serialize(definition))
    } catch (e: JsonProcessingException) {
      throw InvalidRequestException("Unable to convert credentials definition with name $name to JSON", e)
    }
    val account = Account.newRecord().apply {
      id = name
      type = typeName
      body = json
      createdAt = now
      lastModifiedAt = now
      lastModifiedBy = user
    }
    val revision = AccountHistory.newRecord().apply {
      id = name
      type = typeName
      body = json
      lastModifiedAt = now
    }
    return CredentialsRecord(account, revision)
  }

  companion object {
    private fun whoami(): String =
      SecurityContextHolder.getContext().authentication?.name ?: "anonymous"
  }
}

