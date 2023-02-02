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

import org.jooq.*
import org.jooq.impl.CustomRecord
import org.jooq.impl.CustomTable
import org.jooq.impl.DSL.*
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType

val Account = AccountTable()
val AccountHistory = AccountHistoryTable()

class AccountTable : CustomTable<AccountRecord>(name("accounts")) {
  val id: TableField<AccountRecord, String> =
    createField(name("id"), SQLDataType.VARCHAR.nullable(false))
  val type: TableField<AccountRecord, String> =
    createField(name("type"), SQLDataType.VARCHAR.nullable(false))
  val body: TableField<AccountRecord, JSON> =
    createField(name("body"), SQLDataType.JSON.nullable(false))
  val createdAt: TableField<AccountRecord, Long> =
    createField(name("created_at"), SQLDataType.BIGINT.nullable(false))
  val lastModifiedAt: TableField<AccountRecord, Long> =
    createField(name("last_modified_at"), SQLDataType.BIGINT.nullable(false))
  val lastModifiedBy: TableField<AccountRecord, String> =
    createField(name("last_modified_by"), SQLDataType.VARCHAR.nullable(false))

  private val primaryKey = Internal.createUniqueKey(this, id)
  private val identity = Internal.createIdentity(this, id)

  override fun getRecordType(): Class<out AccountRecord> = AccountRecord::class.java
  override fun getPrimaryKey(): UniqueKey<AccountRecord> = primaryKey
  override fun getIdentity(): Identity<AccountRecord, *> = identity
  override fun getKeys(): List<UniqueKey<AccountRecord>> = listOf(primaryKey)
}

class AccountRecord : CustomRecord<AccountRecord>(Account) {
  var id: String
    get() = this[Account.id]
    set(value) {
      this[Account.id] = value
    }

  var type: String
    get() = this[Account.type]
    set(value) {
      this[Account.type] = value
    }

  var body: JSON
    get() = this[Account.body]
    set(value) {
      this[Account.body] = value
    }

  var createdAt: Long
    get() = this[Account.createdAt]
    set(value) {
      this[Account.createdAt] = value
    }

  var lastModifiedAt: Long
    get() = this[Account.lastModifiedAt]
    set(value) {
      this[Account.lastModifiedAt] = value
    }

  var lastModifiedBy: String
    get() = this[Account.lastModifiedBy]
    set(value) {
      this[Account.lastModifiedBy] = value
    }
}

class AccountHistoryTable : CustomTable<AccountHistoryRecord>(name("accounts_history")) {
  val id: TableField<AccountHistoryRecord, String> =
    createField(name("id"), SQLDataType.VARCHAR.nullable(false))
  val type: TableField<AccountHistoryRecord, String?> =
    createField(name("type"), SQLDataType.VARCHAR.nullable(true))
  val body: TableField<AccountHistoryRecord, JSON?> =
    createField(name("body"), SQLDataType.JSON.nullable(true))
  val lastModifiedAt: TableField<AccountHistoryRecord, Long> =
    createField(name("last_modified_at"), SQLDataType.BIGINT.nullable(false))
  val version: TableField<AccountHistoryRecord, Int> =
    createField(name("version"), SQLDataType.INTEGER.nullable(false))
  val deleted: TableField<AccountHistoryRecord, Boolean> =
    createField(name("is_deleted"), SQLDataType.BOOLEAN.nullable(false))
  private val primaryKey = Internal.createUniqueKey(this, id, version)

  override fun getRecordType(): Class<out AccountHistoryRecord> = AccountHistoryRecord::class.java
  override fun getPrimaryKey(): UniqueKey<AccountHistoryRecord> = primaryKey
  override fun getKeys(): List<UniqueKey<AccountHistoryRecord>> = listOf(primaryKey)

  fun nextVersion(name: String): Field<Int> =
    select(count(version) + 1).from(this).where(id.eq(name)).asField()
}

class AccountHistoryRecord : CustomRecord<AccountHistoryRecord>(AccountHistory) {
  var id: String
    get() = this[AccountHistory.id]
    set(value) {
      this[AccountHistory.id] = value
    }

  var type: String?
    get() = this[AccountHistory.type]
    set(value) {
      this[AccountHistory.type] = value
    }

  var body: JSON?
    get() = this[AccountHistory.body]
    set(value) {
      this[AccountHistory.body] = value
    }

  var lastModifiedAt: Long
    get() = this[AccountHistory.lastModifiedAt]
    set(value) {
      this[AccountHistory.lastModifiedAt] = value
    }

  var version: Int
    get() = this[AccountHistory.version]
    set(value) {
      this[AccountHistory.version] = value
    }

  var deleted: Boolean
    get() = this[AccountHistory.deleted]
    set(value) {
      this[AccountHistory.deleted] = value
    }
}
