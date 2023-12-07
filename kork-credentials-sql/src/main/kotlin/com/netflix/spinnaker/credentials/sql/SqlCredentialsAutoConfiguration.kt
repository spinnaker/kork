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

import com.netflix.spinnaker.credentials.ConditionalOnSelfServiceEnabled
import com.netflix.spinnaker.credentials.secrets.CredentialsDefinitionMapper
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@EnableConfigurationProperties
@ConditionalOnSelfServiceEnabled
@ConditionalOnProperty("sql.enabled")
class SqlCredentialsAutoConfiguration {
  @Bean
  @ConfigurationProperties("credentials.sql")
  fun sqlCredentialsDefinitionProperties(): SqlCredentialsDefinitionProperties = SqlCredentialsDefinitionProperties()

  @Bean
  @Primary
  fun sqlCredentialsDefinitionRepository(
    jooq: DSLContext,
    properties: SqlCredentialsDefinitionProperties,
    mapper: CredentialsDefinitionMapper
  ): SqlCredentialsDefinitionRepository = SqlCredentialsDefinitionRepository(jooq, properties, mapper)
}
