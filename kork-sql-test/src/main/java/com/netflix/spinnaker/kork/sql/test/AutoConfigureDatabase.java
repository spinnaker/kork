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

package com.netflix.spinnaker.kork.sql.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping;
import org.springframework.core.annotation.AliasFor;

/**
 * Autoconfiguration for setting up an external database with a Liquibase change log to initialize a
 * fresh database.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ImportAutoConfiguration
@PropertyMapping("spinnaker.test.database")
public @interface AutoConfigureDatabase {
  @AliasFor("databaseName")
  @PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
  String value() default "";

  /** Defines the SQL dialect to use for this test. */
  DatabaseDriver driver() default DatabaseDriver.POSTGRESQL;

  /** Specifies the hostname to connect to. */
  @PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
  String host() default "";

  /** Specifies the port number to connect to. */
  @PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
  int port() default -1;

  /** Defines the name of the database to create for the test. */
  @PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
  String databaseName() default "";

  /** Specifies the username to connect as. */
  @PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
  String username() default "";

  /** Specifies the Liquibase change log file to use to initialize the database. */
  @PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
  String changeLog() default "";
}
