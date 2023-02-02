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

import com.netflix.spinnaker.credentials.CredentialsView
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository.Revision
import com.netflix.spinnaker.kork.exceptions.UserException
import com.netflix.spinnaker.kork.sql.routing.excluded
import com.netflix.spinnaker.kork.sql.routing.readWithPool
import com.netflix.spinnaker.kork.sql.routing.transactWithPool
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException
import io.micrometer.core.annotation.Timed
import org.jooq.*
import org.jooq.exception.DataAccessException
import org.springframework.stereotype.Repository
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*

/**
 * SQL-backed implementation of the [CredentialsDefinitionRepository] API for credentials storage.
 */
@Repository
class SqlCredentialsDefinitionRepository(
  private val jooq: DSLContext,
  private val properties: SqlCredentialsDefinitionProperties,
  private val accountCredentialsMapper: RecordMapper<AccountRecord, CredentialsDefinition>,
  private val accountViewMapper: RecordMapper<AccountRecord, CredentialsView>,
  private val revisionMapper: RecordMapper<AccountHistoryRecord, Revision>,
  private val generator: CredentialsRecordGenerator,
) : CredentialsDefinitionRepository {

  @Timed
  override fun findByName(name: String): Optional<out CredentialsDefinition> = jooq.readWithPool(properties.poolName) {
    selectFrom(Account)
      .where(Account.id.eq(name))
      .fetchOptional(accountCredentialsMapper)
  }

  @Timed
  override fun listByType(typeName: String): List<CredentialsDefinition> = jooq.readWithPool(properties.poolName) {
    selectFrom(Account)
      .where(Account.type.eq(typeName))
      .fetch(accountCredentialsMapper)
      .filterNotNull()
  }

  @Timed
  override fun listCredentialsViews(typeName: String): List<CredentialsView> = jooq.readWithPool(properties.poolName) {
    selectFrom(Account)
      .where(Account.type.eq(typeName))
      .fetch(accountViewMapper)
  }

  @Timed
  override fun create(definition: CredentialsDefinition) {
    val (account, revision) = generator.create(definition)
    val name = definition.name
    jooq.transactWithPool(properties.poolName) {
      try {
        insertInto(Account)
          .set(account)
          .execute()
        insertInto(AccountHistory)
          .set(revision)
          .set(AccountHistory.version, AccountHistory.nextVersion(name))
          .execute()
      } catch (e: DataAccessException) {
        throw e.getCause(SQLIntegrityConstraintViolationException::class.java)?.let {
          UserException("Cannot create duplicate credentials definition using name '$name'", it)
        } ?: UserException("Cannot create credentials definition. ${e.message}", e)
      }
    }
  }

  @Timed
  override fun save(definition: CredentialsDefinition) {
    val (account, revision) = generator.create(definition)
    val name = definition.name
    jooq.transactWithPool(properties.poolName) {
      try {
        insertInto(Account)
          .set(account)
          .run {
            if (dialect() == SQLDialect.POSTGRES) onConflict(Account.id).doUpdate()
            else onDuplicateKeyUpdate()
          }
          .set(Account.body, excluded(Account.body))
          .set(Account.lastModifiedAt, excluded(Account.lastModifiedAt))
          .set(Account.lastModifiedBy, excluded(Account.lastModifiedBy))
          .execute()
        insertInto(AccountHistory)
          .set(revision)
          .set(AccountHistory.version, AccountHistory.nextVersion(name))
          .execute()
      } catch (e: DataAccessException) {
        throw UserException("Cannot save credentials definition. ${e.message}", e)
      }
    }
  }

  @Timed
  override fun saveAll(definitions: Collection<CredentialsDefinition>) {
    val records = generator.create(definitions)
    jooq.transactWithPool(properties.poolName) {
      records.chunked(properties.writeBatchSize) { chunk ->
        var accounts: InsertSetMoreStep<AccountRecord>? = null
        var revisions: InsertSetMoreStep<AccountHistoryRecord>? = null
        chunk.forEach { (account, revision) ->
          accounts = accounts?.run { newRecord().set(account) } ?: insertInto(Account).set(account)
          revisions = (revisions?.run { newRecord().set(revision) } ?: insertInto(AccountHistory).set(revision))
            .set(AccountHistory.version, AccountHistory.nextVersion(account.id))
        }

        accounts?.run {
          (if (dialect() == SQLDialect.POSTGRES) onConflict(Account.id).doUpdate() else onDuplicateKeyUpdate())
            .set(Account.body, excluded(Account.body))
            .set(Account.lastModifiedAt, excluded(Account.lastModifiedAt))
            .set(Account.lastModifiedBy, excluded(Account.lastModifiedBy))
            .execute()
        }
        revisions?.execute()
      }
    }
  }

  @Timed
  override fun update(definition: CredentialsDefinition) {
    val (account, revision) = generator.create(definition)
    val name = definition.name
    jooq.transactWithPool(properties.poolName) {
      try {
        val rows = update(Account)
          .set(Account.body, account.body)
          .set(Account.lastModifiedAt, account.lastModifiedAt)
          .set(Account.lastModifiedBy, account.lastModifiedBy)
          .where(Account.id.eq(name))
          .execute()
        if (rows != 1) {
          throw NotFoundException("Cannot update credentials definition with name '$name' as it does not exist")
        }
        insertInto(AccountHistory)
          .set(revision)
          .set(AccountHistory.version, AccountHistory.nextVersion(name))
          .execute()
      } catch (e: DataAccessException) {
        throw UserException("Cannot update credentials definition. ${e.message}", e)
      }
    }
  }

  @Timed
  override fun delete(name: String) {
    jooq.transactWithPool(properties.poolName) {
      try {
        val accountDoesNotExists = selectFrom(Account)
          .where(Account.id.eq(name))
          .forUpdate().of(Account)
          .fetchOptional()
          .isEmpty
        if (accountDoesNotExists) {
          throw NotFoundException("No credentials definition found with name '$name'")
        }
        val now = configuration().clock().millis()
        val revision = AccountHistory.newRecord().apply {
          id = name
          deleted = true
          lastModifiedAt = now
        }
        insertInto(AccountHistory)
          .set(revision)
          .set(AccountHistory.version, AccountHistory.nextVersion(name))
          .execute()
        deleteFrom(Account)
          .where(Account.id.eq(name))
          .execute()
      } catch (e: DataAccessException) {
        throw UserException("Unable to delete credentials definition with name '$name'. ${e.message}", e)
      }
    }
  }

  @Timed
  override fun deleteAll(names: Collection<String>) {
    jooq.transactWithPool(properties.poolName) {
      val now = configuration().clock().millis()
      names.chunked(properties.writeBatchSize) { chunk ->
        val matchingAccounts = try {
          selectFrom(Account)
            .where(Account.id.`in`(chunk))
            .forUpdate().of(Account)
            .fetchSet(Account.id)
        } catch (e: DataAccessException) {
          throw UserException("Unable to delete credentials definition(s) with name(s) $chunk. ${e.message}", e)
        }
        if (matchingAccounts.size < chunk.size) {
          val missingAccounts = chunk.toSet() - matchingAccounts
          throw NotFoundException("Unable to find account(s): $missingAccounts")
        }
        var histories: InsertSetMoreStep<AccountHistoryRecord>? = null
        chunk.forEach { name ->
          histories = (histories?.run { newRecord() } ?: insertInto(AccountHistory))
            .set(AccountHistory.id, name)
            .set(AccountHistory.deleted, true)
            .set(AccountHistory.lastModifiedAt, now)
            .set(AccountHistory.version, AccountHistory.nextVersion(name))
        }
        try {
          histories?.execute()
          deleteFrom(Account)
            .where(Account.id.`in`(chunk))
            .execute()
        } catch (e: DataAccessException) {
          throw UserException("Unable to delete credentials definitions(s). ${e.message}", e)
        }
      }
    }
  }

  @Timed
  override fun revisionHistory(name: String): List<Revision> =
    jooq.readWithPool(properties.poolName) {
      selectFrom(AccountHistory)
        .where(AccountHistory.id.eq(name))
        .orderBy(AccountHistory.version.desc())
        .fetch(revisionMapper)
    }
}
