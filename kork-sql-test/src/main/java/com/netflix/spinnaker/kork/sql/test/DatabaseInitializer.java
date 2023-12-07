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

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({TransactionAutoConfiguration.class, JooqAutoConfiguration.class})
@ConditionalOnProperty(prefix = "spinnaker.test.database", name = "database-name")
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseInitializer {
  private final DatabaseProperties properties;

  public DatabaseInitializer(DatabaseProperties properties) {
    this.properties = properties;
  }

  @Bean
  public DataSourceInitializer testDatabaseInitializer() {
    var driver = properties.getDriver();
    // use the default database for the connecting user to bootstrap a fresh database
    var url =
        String.format(
            "jdbc:%s://%s:%d/", driver.getId(), properties.getHost(), properties.getPort());
    var bootstrapDataSource = new SingleConnectionDataSource();
    bootstrapDataSource.setUrl(url);
    bootstrapDataSource.setUsername(properties.getUsername());
    bootstrapDataSource.setDriverClassName(driver.getDriverClassName());
    var initializer = new DataSourceInitializer();
    initializer.setDataSource(bootstrapDataSource);
    String databaseName = properties.getDatabaseName();
    initializer.setDatabasePopulator(
        connection -> connection.createStatement().execute("create database " + databaseName));
    initializer.setDatabaseCleaner(
        connection -> connection.createStatement().execute("drop database " + databaseName));
    return initializer;
  }

  @Bean
  @DependsOn("testDatabaseInitializer")
  public DataSource dataSource() {
    var driver = properties.getDriver();
    var url =
        String.format(
            "jdbc:%s://%s:%d/%s",
            driver.getId(),
            properties.getHost(),
            properties.getPort(),
            properties.getDatabaseName());
    return DataSourceBuilder.create()
        .url(url)
        .driverClassName(driver.getDriverClassName())
        .username(properties.getUsername())
        .build();
  }

  @Bean
  public SpringLiquibase liquibase(DataSource dataSource) {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(properties.getChangeLog());
    return liquibase;
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  public DSLContext dslContext(DataSource dataSource) {
    return new DefaultDSLContext(
        new TransactionAwareDataSourceProxy(dataSource), convertToDialect(properties.getDriver()));
  }

  private static SQLDialect convertToDialect(DatabaseDriver driver) {
    switch (driver) {
      case MYSQL:
        return SQLDialect.MYSQL;
      case MARIADB:
        return SQLDialect.MARIADB;
      case POSTGRESQL:
        return SQLDialect.POSTGRES;
      case H2:
        return SQLDialect.H2;
      case DERBY:
        return SQLDialect.DERBY;
      case HSQLDB:
        return SQLDialect.HSQLDB;
      case SQLITE:
        return SQLDialect.SQLITE;
      default:
        throw new UnsupportedOperationException("Cannot map driver to jOOQ dialect: " + driver);
    }
  }
}
