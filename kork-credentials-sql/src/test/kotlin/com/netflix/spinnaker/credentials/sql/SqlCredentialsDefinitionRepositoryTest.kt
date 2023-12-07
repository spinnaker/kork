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

import com.netflix.spinnaker.credentials.CredentialsAutoConfiguration
import com.netflix.spinnaker.credentials.CredentialsSource
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository
import com.netflix.spinnaker.credentials.test.TestCredentialsDefinition
import com.netflix.spinnaker.kork.sql.test.AutoConfigureDatabase
import com.netflix.spinnaker.security.Authorization
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.test.context.support.WithMockUser
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isContainedIn
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.time.Instant

@SpringBootTest(
  properties = [
    "sql.enabled=true",
    "account.storage.enabled=true",
    "spinnaker.test.database.host=\${SQL_HOST:localhost}",
    "spinnaker.test.database.port=\${PG_PORT:5432}",
    "spinnaker.test.database.username=postgres",
    "spinnaker.test.database.change-log=classpath:/db/changelog/accounts.yml",
  ],
  classes = [SqlCredentialsAutoConfiguration::class, CredentialsAutoConfiguration::class]
)
@AutoConfigureDatabase("credentials_repository")
@AutoConfigureJson
@WithMockUser
class SqlCredentialsDefinitionRepositoryTest {
  @MockBean
  lateinit var permissionEvaluator: PermissionEvaluator

  @Autowired
  lateinit var repository: CredentialsDefinitionRepository

  private val accountName = "test-account"
  private val accountType = "test"

  @Test
  fun smokeTest() {
    val credentials =
      TestCredentialsDefinition(
        name = accountName,
        attributes = mapOf("hello" to "world"),
        permissions = mapOf(Authorization.READ to setOf("USER"), Authorization.WRITE to setOf("USER"))
      )

    val metadata = repository.create(credentials)

    expectThat(metadata) {
      get { type } isEqualTo accountType
      get { name } isEqualTo accountName
      get { source } isEqualTo CredentialsSource.STORAGE
      get { lastModified }.isNotNull().isLessThan(Instant.now())
      get { eTag }.isNotNull()
    }

    val result = repository.findByName(accountName)

    expectThat(result) {
      isNotNull()
      isA<TestCredentialsDefinition>().get { attributes["hello"] } isEqualTo "world"
    }

    val eTag = metadata.eTag
    val updatedCredentials = credentials.copy(attributes = mapOf("hello" to "brooklyn"))
    val updatedMetadata = repository.updateIfMatch(updatedCredentials, listOf(eTag))

    expectThat(updatedMetadata) {
      get { this.eTag } isNotEqualTo eTag
    }

    val updatedResult = repository.findByName(accountName)

    expectThat(updatedResult) {
      isNotNull()
      isA<TestCredentialsDefinition>().get { attributes["hello"] } isEqualTo "brooklyn"
    }

    repository.delete(accountName)

    val deletedResult = repository.findByName(accountName)

    expectThat(deletedResult) {
      isNull()
    }

    val history = repository.revisionHistory(accountName)

    expectThat(history) {
      hasSize(3) // create, update, delete
      all { get { user } isEqualTo "user" }
    }
  }

  @Test
  fun bulkSmokeTest() {
    val credentialsList = (1..5).map {
      TestCredentialsDefinition(
        name = "test-account-$it",
        attributes = mapOf("iteration" to it.toString()),
        permissions = mapOf(Authorization.READ to setOf("USER"), Authorization.WRITE to setOf("USER"))
      )
    }

    val views = repository.saveAll(credentialsList)
    expectThat(views) {
      hasSize(credentialsList.size)
      all {
        get { spec }.isA<TestCredentialsDefinition>() isContainedIn credentialsList
        and { get { status.isValid }.isTrue() }
      }
    }

    val saved = repository.listByType(accountType)
    expectThat(saved) {
      containsExactlyInAnyOrder(credentialsList)
    }

    val toDelete = credentialsList.map { it.name }
    repository.deleteAll(toDelete)

    val remaining = repository.listByType(accountType)
    expectThat(remaining) {
      isEmpty()
    }
  }
}
