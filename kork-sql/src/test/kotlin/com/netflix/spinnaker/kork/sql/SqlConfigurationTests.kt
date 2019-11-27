/*
 * Copyright 2019 Netflix, Inc.
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
 */

package com.netflix.spinnaker.kork.sql

import com.netflix.spinnaker.kork.PlatformComponents
import com.netflix.spinnaker.kork.sql.config.DefaultSqlConfiguration
import com.netflix.spinnaker.kork.sql.config.SqlProperties
import org.jooq.DSLContext
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = [SqlConfigTestApp::class]
)
internal class SqlConfigurationTests {

  @Autowired
  lateinit var applicationContext: ApplicationContext

  @Autowired
  lateinit var sqlProperties: SqlProperties

  @Test
  fun `should have 2 JOOQ configured for both H2 & MYSQL pool`() {
    expectThat(applicationContext.getBeansOfType(DSLContext::class.java).size).isEqualTo(2)
    expectThat(sqlProperties.connectionPools.size).isEqualTo(2)
    expectThat(applicationContext.getBean("jooq")).isNotNull()
    expectThat(applicationContext.getBean("secondaryJooq")).isNotNull()
    expectThat(applicationContext.getBean("liquibase")).isNotNull()
    expectThat(applicationContext.getBean("secondaryLiquibase")).isNotNull()
  }
}

@SpringBootApplication
@Import(PlatformComponents::class, DefaultSqlConfiguration::class)
internal class SqlConfigTestApp
