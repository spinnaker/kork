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

import com.netflix.spinnaker.credentials.definition.error.CredentialsDefinitionRepositoryException
import com.netflix.spinnaker.credentials.definition.error.DuplicateCredentialsDefinitionException
import com.netflix.spinnaker.credentials.definition.error.GenericCredentialsDefinitionRepositoryException
import com.netflix.spinnaker.credentials.definition.error.NoMatchingCredentialsException
import com.netflix.spinnaker.credentials.definition.error.NoSuchCredentialsDefinitionException
import com.netflix.spinnaker.credentials.sql.SqlCredentialsDefinitionRepository.Companion.logger
import com.netflix.spinnaker.kork.exceptions.InvalidCredentialsTypeException
import com.netflix.spinnaker.kork.sql.routing.excluded
import com.netflix.spinnaker.kork.sql.routing.transactional
import com.netflix.spinnaker.kork.sql.routing.transactionalResult
import org.jooq.*
import org.jooq.exception.DataAccessException
import org.jooq.exception.NoDataFoundException
import org.jooq.impl.DSL.*
import java.sql.SQLIntegrityConstraintViolationException
import com.netflix.spinnaker.credentials.CredentialsView.Metadata as CredentialsMetadata

/**
 * Inserts an account record if it does not exist and returns its entity tag. This synchronizes the
 * account history, too.
 *
 * @throws DataAccessException if an error occurs inserting the account or its history
 */
internal fun createAccount(account: AccountRecord): TransactionalCallable<CredentialsMetadata> = transactionalResult {
  val metadata = with(insertQuery(Account)) {
    setRecord(account)
    setReturning()
    execute()
    returnedRecord?.intoMetadata()
      ?: throw NoDataFoundException("Unable to fetch metadata of newly inserted credentials for name '${account.id}'")
  }
  execute(syncAccountHistory(listOf(account.id)))
  metadata
}

/**
 * Converts a [DataAccessException] into an appropriate [CredentialsDefinitionRepositoryException] and throws it.
 */
internal fun DataAccessException.rethrowAsCredentialsDefinitionRepositoryException(
  ctx: DSLContext, id: String
): Nothing = throw getCause(SQLIntegrityConstraintViolationException::class.java)?.let {
  try {
    ctx.selectFrom(Account)
      .where(Account.id.eq(id))
      .fetchSingle()
      .run {
        val existing = CredentialsData(body)
        DuplicateCredentialsDefinitionException(existing, id, eTag, lastModifiedAt, it)
      }
  } catch (e: DataAccessException) {
    GenericCredentialsDefinitionRepositoryException(id, "Error while looking up existing credentials: $message", e)
  }
} ?: GenericCredentialsDefinitionRepositoryException(id, "Error while saving credentials: $message", this)

/**
 * Saves a single account record by creating or updating an account row and syncing its history.
 *
 * @throws DataAccessException if an error occurs while saving the account or its history
 */
internal fun saveAccount(account: AccountRecord): TransactionalCallable<CredentialsMetadata> = transactionalResult {
  with(insertQuery(Account)) {
    setRecord(account)
    onDuplicateKeyUpdate(true)
    addValuesForUpdate(accountValuesForUpsert)
    setReturning()
    execute()
    returnedRecord?.intoMetadata()
      ?: throw NoDataFoundException("Unable to fetch updated metadata for credentials with name '${account.id}'")
  }.also {
    execute(syncAccountHistory(listOf(account.id)))
  }
}

/**
 * Saves multiple accounts by creating or updating account rows using the provided records. New versions
 * will be set on the records along with synchronizing account history.
 *
 * @param accounts batch of account records to save
 * @return map of metadata for saved accounts keyed by name
 * @throws DataAccessException if an error occurs saving any account or history
 */
internal fun saveAccounts(accounts: Iterable<AccountRecord>): TransactionalCallable<Map<String, CredentialsMetadata>> =
  transactionalResult {
    with(insertQuery(Account)) {
      accounts.forEach(::addRecord)
      onDuplicateKeyUpdate(true)
      addValuesForUpdate(accountValuesForUpsert)
      setReturning()
      execute()
      returnedRecords.intoMap(Account.id, AccountRecord::intoMetadata)
    }.also {
      execute(syncAccountHistory(it.keys))
    }
  }

private val Scope.accountValuesForUpsert: Map<*, *>
  get() = listOf(
    Account.type, Account.body, Account.lastModifiedAt, Account.lastModifiedBy
  ).associateWith { excluded(it) }

/**
 * Updates an account using the provided record if it exists and returns its entity tag.
 *
 * @throws NoDataFoundException if no account exists with the given name
 * @throws InvalidCredentialsTypeException if the given account type does not match its existing type
 * @throws DataAccessException if an error occurs updating the account or synchronizing its history
 */
internal fun updateAccount(account: AccountRecord): TransactionalCallable<CredentialsMetadata> = transactionalResult {
  val condition = Account.id.eq(account.id)
  val existingType =
    select(Account.type).from(Account).where(condition).forUpdate().of(Account).fetchSingleInto(String::class.java)
  val proposedType = account.type
  validateAccountType(account.id, proposedType, existingType)
  updateAccount(account)
}

/**
 * Updates an account using the provided record only if it exists and has an entity tag matching one of the
 * provided matches.
 *
 * @throws NoDataFoundException if no account exists with the given name
 * @throws InvalidCredentialsTypeException if the given account type does not match its existing type
 * @throws DataAccessException if an error occurs updating the account or synchronizing its history
 * @throws NoMatchingCredentialsException if an account exists with the given name but does not match any of the given
 * entity tags
 */
internal fun updateAccountIfMatches(
  account: AccountRecord, ifMatches: List<String>
): TransactionalCallable<CredentialsMetadata> = transactionalResult {
  logger.debug { "Attempting to update ${account.id} with '${account.body.data()}' using if-match $ifMatches" }
  val condition = Account.id.eq(account.id)
  val existing = selectFrom(Account).where(condition).forUpdate().of(Account).fetchSingle()
  val proposedType = account.type
  val existingType = existing.type
  validateAccountType(account.id, proposedType, existingType)
  if (existing.eTag !in ifMatches) {
    logger.warn { "Unable to conditionally update credentials for ${account.id} as its etag ${existing.eTag} is not in $ifMatches" }
    throw NoMatchingCredentialsException(
      CredentialsData(existing.body), existing.id, existing.eTag, existing.lastModifiedAt
    )
  }
  updateAccount(account)
}

private fun validateAccountType(name: String, proposedType: String, existingType: String) {
  if (proposedType != existingType) {
    logger.warn { "Unable to update credentials for $name as the existing type $existingType does not match the proposed type $proposedType" }
    throw InvalidCredentialsTypeException("Existing credentials type $existingType does not match proposed type $proposedType").apply {
      additionalAttributes["name"] = name
    }
  }
}

private fun DSLContext.updateAccount(account: AccountRecord): CredentialsMetadata =
  with(updateQuery(Account)) {
    setRecord(account)
    addValue(Account.lastModifiedAt, currentInstant())
    addConditions(Account.id.eq(account.id))
    setReturning()
    execute()
    returnedRecord?.intoMetadata()
      ?: throw NoDataFoundException("Unable to fetch updated metadata for credentials with name '${account.id}'")
  }.also {
    execute(syncAccountHistory(listOf(account.id)))
  }

private fun syncAccountHistory(ids: Collection<String>): Query =
  insertInto(AccountHistory)
    .columns(
      AccountHistory.id,
      AccountHistory.type,
      AccountHistory.body,
      AccountHistory.lastModifiedAt,
      AccountHistory.modifiedBy
    )
    .select(
      select(
        Account.id,
        Account.type,
        Account.body,
        Account.lastModifiedAt,
        Account.lastModifiedBy
      )
        .from(Account)
        .where(Account.id.`in`(ids))
    )

/**
 * Deletes an account by name and synchronizes its account history.
 *
 * @throws NoDataFoundException if no account exists with the given name
 * @throws DataAccessException if an error occurs while deleting the account or syncing history
 */
internal fun deleteAccount(id: String): TransactionalRunnable = transactional {
  val condition = Account.id.eq(id)
  val exists = select(value(1))
    .from(Account)
    .where(condition)
    .forUpdate().of(Account)
    .fetchSingleInto(Int::class.java)
  // should already have thrown an exception
  check(exists > 0)
  with(insertQuery(AccountHistory)) {
    addValue(AccountHistory.id, id)
    addValue(AccountHistory.deleted, true)
    addValue(AccountHistory.modifiedBy, currentUser)
    execute()
  }
  with(deleteQuery(Account)) {
    addConditions(condition)
    execute()
  }
}

/**
 * Deletes a batch of accounts by name and synchronizes their history.
 *
 * @throws NoSuchCredentialsDefinitionException if one or more of the requested accounts do not exist
 * @throws DataAccessException if an error occurs while deleting the accounts or syncing histories
 */
internal fun deleteAccounts(ids: Collection<String>): TransactionalRunnable = transactional {
  val condition = Account.id.`in`(ids)
  val deleted = with(deleteQuery(Account)) {
    addConditions(condition)
    execute()
  }
  if (deleted != ids.size) {
    val foundIds = select(Account.id)
      .from(Account)
      .where(condition)
      .fetchSet(Account.id)
    val missingIds = ids.toHashSet().apply { removeAll(foundIds) }
    if (missingIds.isNotEmpty()) {
      throw NoSuchCredentialsDefinitionException(missingIds, "Unable to delete non-existent credentials")
    }
  }
  val user = currentUser
  with(insertQuery(AccountHistory)) {
    ids.forEach { id ->
      newRecord()
      addValue(AccountHistory.id, id)
      addValue(AccountHistory.deleted, true)
      addValue(AccountHistory.modifiedBy, user)
    }
    execute()
  }
}
