/*
 * Copyright 2019 Netflix, Inc.
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
package com.netflix.spinnaker.kork.sql.routing

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SQLDialect
import org.jooq.Scope
import org.jooq.TransactionalCallable
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.function
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.unquotedName
import org.jooq.impl.DSL.using

/**
 * Convenience method for use with jOOQ queries, defining the connection pool to use by name. If the requested
 * connection pool does not exist, the configured default connection pool will be used.
 *
 * ```
 * val result = withPool("myPool") {
 *   jooq.select(...).fetchOne()
 * }
 * ```
 *
 * @param name The name of the connection pool
 * @param callback The code to execute with the provided connection pool targeted
 */
inline fun <T> withPool(name: String, callback: () -> T): T {
  NamedDatabaseContextHolder.set(name)
  try {
    return callback()
  } finally {
    NamedDatabaseContextHolder.clear()
  }
}

/**
 * Convenience function to perform a read query in a particular connection pool.
 *
 * ```
 * val result = jooq.readWithPool("myPool") {
 *   select(...).from(...).fetch()
 * }
 * ```
 */
fun <T> DSLContext.readWithPool(name: String, callback: DSLContext.() -> T): T =
  withPool(name) {
    with(this, callback)
  }

/**
 * Convenience function to create a [TransactionalCallable].
 */
inline fun <T> transactionalResult(crossinline callback: DSLContext.() -> T): TransactionalCallable<T> =
  TransactionalCallable {
    with(using(it), callback)
  }

/**
 * Convenience function to create a [TransactionalRunnable].
 */
inline fun transactional(crossinline callback: DSLContext.() -> Unit): TransactionalRunnable =
  TransactionalRunnable {
    with(using(it), callback)
  }

/**
 * Generates an excluded field reference for use with `ON CONFLICT (key) DO UPDATE` or
 * `ON DUPLICATE KEY DO UPDATE` style queries. This translates a column into the column
 * corresponding to the proposed update value rather than the existing value of the same
 * column.
 */
fun <T> Scope.excluded(field: Field<T>): Field<T> = when (dialect()) {
  SQLDialect.POSTGRES -> field(name(unquotedName("excluded"), field.unqualifiedName), field.dataType)
  else -> function("values", field.dataType, field)
}
