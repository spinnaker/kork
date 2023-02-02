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
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionMapper
import org.jooq.RecordMapper
import org.springframework.stereotype.Component

/**
 * [RecordMapper] for loading and validating credentials.
 */
@Component
class AccountRecordCredentialsViewMapper(private val mapper: CredentialsDefinitionMapper) :
  RecordMapper<AccountRecord, CredentialsView> {
  override fun map(record: AccountRecord): CredentialsView =
    mapper.deserializeWithErrors(record.body.data()).apply {
      metadata.source = CredentialsView.Source.STORAGE
    }
}
