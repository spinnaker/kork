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

import com.netflix.spinnaker.credentials.CredentialsTypes
import com.netflix.spinnaker.credentials.CredentialsView
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository.Revision
import com.netflix.spinnaker.credentials.definition.error.GenericCredentialsDefinitionRepositoryException
import com.netflix.spinnaker.credentials.definition.error.NoSuchCredentialsDefinitionException
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionMapper
import com.netflix.spinnaker.kork.sql.routing.readWithPool
import com.netflix.spinnaker.kork.sql.routing.withPool
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.DSLContext
import org.jooq.TransactionalRunnable
import org.jooq.exception.DataAccessException
import org.jooq.exception.NoDataFoundException

// class must be open to allow for CGLIB proxying
open class SqlCredentialsDefinitionRepository(
  private val jooq: DSLContext,
  private val properties: SqlCredentialsDefinitionProperties,
  private val mapper: CredentialsDefinitionMapper,
) : CredentialsDefinitionRepository {

  @Timed("credentials.find.definition")
  override fun findByName(name: String): CredentialsDefinition? =
    jooq.readWithPool(properties.poolName) {
      selectFrom(Account)
        .where(Account.id.eq(name))
        .fetchOne(mapper::deserializeWithSecrets)
    }

  @Timed("credentials.find.metadata")
  override fun findMetadataByName(name: String): CredentialsView.Metadata? =
    jooq.readWithPool(properties.poolName) {
      selectFrom(Account)
        .where(Account.id.eq(name))
        .fetchOne(AccountRecord::intoMetadata)
    }

  @Timed("credentials.list.type.name")
  override fun listByType(typeName: String): List<CredentialsDefinition> =
    jooq.readWithPool(properties.poolName) {
      selectFrom(Account)
        .where(Account.type.eq(typeName))
        .fetch(mapper::deserializeWithSecrets)
        .filterNotNull()
    }

  @Timed("credentials.list.type.class")
  override fun <T : CredentialsDefinition?> listByType(type: Class<T>): List<T> =
    jooq.readWithPool(properties.poolName) {
      selectFrom(Account)
        .where(Account.type.eq(CredentialsTypes.getRequiredCredentialsTypeName(type)))
        .fetch(mapper::deserializeWithSecrets)
        .filterNotNull()
        .map(type::cast)
    }

  @Timed("credentials.list.view")
  override fun listCredentialsViews(typeName: String): List<CredentialsView> = jooq.readWithPool(properties.poolName) {
    selectFrom(Account)
      .where(Account.type.eq(typeName))
      .fetch(mapper::deserializeToValidatingView)
  }

  @Timed("credentials.create")
  override fun create(definition: CredentialsDefinition): CredentialsView.Metadata {
    val record = Account.newRecord().apply {
      id = definition.name
      type = definition.type
      body = mapper.serializeIntoWrapper(definition)
      lastModifiedBy = currentUser
    }
    return withPool(properties.poolName) {
      try {
        jooq.transactionResult(createAccount(record))
      } catch (e: DataAccessException) {
        e.rethrowAsCredentialsDefinitionRepositoryException(jooq, record.id)
      }
    }
  }

  @Timed("credentials.save.single")
  override fun save(definition: CredentialsDefinition): CredentialsView.Metadata {
    val record = Account.newRecord().apply {
      id = definition.name
      type = definition.type
      body = mapper.serializeIntoWrapper(definition)
      lastModifiedBy = currentUser
    }
    return withPool(properties.poolName) {
      try {
        jooq.transactionResult(saveAccount(record))
      } catch (e: DataAccessException) {
        throw GenericCredentialsDefinitionRepositoryException(
          definition.name, "Error while trying to save credentials: ${e.message}", e
        )
      }
    }
  }

  @Timed("credentials.save.multi")
  override fun saveAll(definitions: Collection<CredentialsDefinition>): List<CredentialsView> {
    val user = currentUser
    val records = definitions.map {
      Account.newRecord().apply {
        id = it.name
        type = it.type
        body = mapper.serializeIntoWrapper(it)
        lastModifiedBy = user
      }
    }
    val metadata = try {
      jooq.transactionResult { cfg ->
        records.chunked(properties.writeBatchSize, ::saveAccounts)
          .flatMap { it.run(cfg).entries }
          .associate { it.toPair() }
      }
    } catch (e: DataAccessException) {
      throw GenericCredentialsDefinitionRepositoryException(
        definitions.map(CredentialsDefinition::getName),
        "Error while trying to save batch of credentials: ${e.message}",
        e
      )
    }
    return definitions.map { CredentialsView(metadata.getValue(it.name), it) }
  }

  @Timed("credentials.update.single")
  override fun update(definition: CredentialsDefinition): CredentialsView.Metadata {
    val name = definition.name
    val record = Account.newRecord().apply {
      id = name
      type = definition.type
      body = mapper.serializeIntoWrapper(definition)
      lastModifiedBy = currentUser
    }
    return try {
      jooq.transactionResult(updateAccount(record))
    } catch (e: NoDataFoundException) {
      throw NoSuchCredentialsDefinitionException(name, "Cannot update credentials that do not exist", e)
    } catch (e: DataAccessException) {
      throw GenericCredentialsDefinitionRepositoryException(
        name, "Error while trying to update credentials: ${e.message}", e
      )
    }
  }

  @Timed("credentials.update.conditional")
  override fun updateIfMatch(definition: CredentialsDefinition, ifMatches: List<String>): CredentialsView.Metadata {
    val name = definition.name
    val record = Account.newRecord().apply {
      id = name
      type = definition.type
      body = mapper.serializeIntoWrapper(definition)
      lastModifiedBy = currentUser
    }
    return try {
      jooq.transactionResult(updateAccountIfMatches(record, ifMatches))
    } catch (e: NoDataFoundException) {
      throw NoSuchCredentialsDefinitionException(
        name, "Cannot update credentials that do not exist", e
      )
    } catch (e: DataAccessException) {
      throw GenericCredentialsDefinitionRepositoryException(
        name,
        "Error while conditionally updating credentials: ${e.message}",
        e
      )
    }
  }

  @Timed("credentials.list.unknown")
  override fun getUnknownNames(names: Collection<String>): Set<String> = jooq.readWithPool(properties.poolName) {
    val foundIds = select(Account.id).from(Account).where(Account.id.`in`(names)).fetchSet(Account.id)
    names.toHashSet().apply { removeAll(foundIds) }
  }

  @Timed("credentials.delete.single")
  override fun delete(name: String) {
    try {
      jooq.transaction(deleteAccount(name))
    } catch (e: NoDataFoundException) {
      throw NoSuchCredentialsDefinitionException(name, "Cannot delete credentials that do not exist", e)
    } catch (e: DataAccessException) {
      throw GenericCredentialsDefinitionRepositoryException(
        name, "Error while trying to delete credentials: ${e.message}", e
      )
    }
  }

  @Timed("credentials.delete.multi")
  override fun deleteAll(names: Collection<String>) {
    val transaction = TransactionalRunnable.of(names.chunked(properties.writeBatchSize, ::deleteAccounts))
    withPool(properties.poolName) {
      try {
        jooq.transaction(transaction)
      } catch (e: DataAccessException) {
        throw GenericCredentialsDefinitionRepositoryException(
          names, "Error while trying to delete batch of credentials: ${e.message}", e
        )
      }
    }
  }

  @Timed("credentials.list.history")
  override fun revisionHistory(name: String): List<Revision> =
    jooq.readWithPool(properties.poolName) {
      selectFrom(AccountHistory)
        .where(AccountHistory.id.eq(name))
        .orderBy(AccountHistory.revision.desc())
        .fetch(mapper::deserializeToRevision)
    }

  companion object : Logging
}
