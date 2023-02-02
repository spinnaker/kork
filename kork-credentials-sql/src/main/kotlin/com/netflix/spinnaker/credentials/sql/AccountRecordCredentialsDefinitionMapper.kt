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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionMapper
import com.netflix.spinnaker.kork.secrets.SecretException
import io.micrometer.core.instrument.Metrics
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.RecordMapper
import org.springframework.stereotype.Component

/**
 * [RecordMapper] for loading and decrypting credentials.
 */
@Component
class AccountRecordCredentialsDefinitionMapper(
  private val mapper: CredentialsDefinitionMapper,
) : RecordMapper<AccountRecord, CredentialsDefinition>, Logging {
  private val secretLoadError = Metrics.counter("credentials.sql.secretLoadError")
  private val dataLoadError = Metrics.counter("credentials.sql.dataLoadError")
  override fun map(record: AccountRecord): CredentialsDefinition? = try {
    mapper.deserializeWithSecrets(record.body.data())
  } catch (e: SecretException) {
    logger.info(e) { "Unable to decrypt secret(s) from account ${record.id}" }
    secretLoadError.increment()
    null
  } catch (e: Exception) {
    logger.warn(e) { "Unable to parse account ${record.id}. This account may be damaged." }
    dataLoadError.increment()
    null
  }
}
