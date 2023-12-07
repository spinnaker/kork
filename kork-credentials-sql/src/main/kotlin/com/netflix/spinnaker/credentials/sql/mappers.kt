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
 *
 */

package com.netflix.spinnaker.credentials.sql

import com.fasterxml.jackson.core.JsonProcessingException
import com.netflix.spinnaker.credentials.CredentialsSource
import com.netflix.spinnaker.credentials.CredentialsView
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository.Revision
import com.netflix.spinnaker.credentials.definition.error.InvalidCredentialsDefinitionException
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionMapper
import com.netflix.spinnaker.kork.exceptions.InvalidCredentialsTypeException
import com.netflix.spinnaker.kork.secrets.SecretException
import io.micrometer.core.instrument.Metrics
import org.apache.logging.log4j.kotlin.loggerOf
import org.jooq.JSON
import org.jooq.JSON.json
import java.lang.IllegalArgumentException

private val logger = loggerOf(CredentialsDefinitionMapper::class.java)
private val errorsDeserializingAccounts = Metrics.counter("credentials.sql.account.deserialization.errors")
private val errorsDeserializingRevisions = Metrics.counter("credentials.sql.revision.deserialization.errors")
private val errorsDecryptingSecrets = Metrics.counter("credentials.sql.secret.decryption.errors")
private val errorsSerializingAccounts = Metrics.counter("credentials.sql.account.serialization.errors")

/**
 * Record mapper for translating account records into credentials definitions. This fetches and decrypts
 * referenced secrets.
 */
internal fun CredentialsDefinitionMapper.deserializeWithSecrets(record: AccountRecord): CredentialsDefinition? = try {
  deserializeWithSecrets(record.body.data())
} catch (e: SecretException) {
  logger.info(e) { "Unable to decrypt secret from credentials with type ${record.type}, name '${record.id}'" } // , and version ${record.version}
  errorsDecryptingSecrets.increment()
  null
} catch (e: JsonProcessingException) {
  logger.warn(e) { "Unable to parse credentials with type ${record.type}, name '${record.id}'" } // , and version ${record.version}
  errorsDeserializingAccounts.increment()
  null
} catch (e: InvalidCredentialsTypeException) {
  logger.warn(e) { "Unable to parse credentials with type ${record.type}, name '${record.id}" }
  errorsDeserializingAccounts.increment()
  null
} catch (e: IllegalArgumentException) {
  logger.warn(e) { "Unable to parse credentials with type ${record.type}, name '${record.id}" }
  errorsDeserializingAccounts.increment()
  null
}

/**
 * Record mapper for translating account records into validated views of credentials. This validates but does
 * not substitute referenced secrets.
 */
internal fun CredentialsDefinitionMapper.deserializeToValidatingView(record: AccountRecord): CredentialsView =
  deserializeWithErrors(record.body.data()).apply {
    metadata.apply {
      if (name == null) {
        name = record.id
      }
      if (type == null) {
        type = record.type
      }
      eTag = record.eTag
      lastModified = record.lastModifiedAt
      source = CredentialsSource.STORAGE
    }
  }

/**
 * Record mapper for translating account history records into revisions. This does not attempt to validate or
 * decrypt any referenced secrets.
 */
internal fun CredentialsDefinitionMapper.deserializeToRevision(record: AccountHistoryRecord): Revision = record.body?.let {
  try {
    deserialize(it.data())
  } catch (e: JsonProcessingException) {
    logger.warn(e) { "Unable to parse credentials revision with type ${record.type}, name '${record.id}', and revision ${record.revision}" }
    errorsDeserializingRevisions.increment()
    null
  }
}.let { Revision(record.revision, record.lastModifiedAt, it, record.modifiedBy) }

/**
 * Serializes the provided credentials definition into a wrapped [JSON] instance.
 *
 * @throws InvalidCredentialsDefinitionException if there is an error serializing the credentials
 * @throws InvalidCredentialsTypeException if the provided credentials are not annotated with `@CredentialsType`
 */
internal fun CredentialsDefinitionMapper.serializeIntoWrapper(definition: CredentialsDefinition): JSON = try {
  json(serialize(definition))
} catch (e: JsonProcessingException) {
  errorsSerializingAccounts.increment()
  throw InvalidCredentialsDefinitionException(
    definition.name, "Error while serializing credentials into JSON: ${e.message}", e
  )
}
