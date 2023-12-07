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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.jdbc.DatabaseDriver;

@ConstructorBinding
@ConfigurationProperties("spinnaker.test.database")
public class DatabaseProperties {
  private final DatabaseDriver driver;
  private final String host;
  private final int port;
  private final String databaseName;
  private final String username;
  private final String changeLog;

  public DatabaseProperties(
      DatabaseDriver driver,
      String host,
      int port,
      String databaseName,
      String username,
      String changeLog) {
    this.driver = driver;
    this.host = host;
    this.port = port;
    this.databaseName = databaseName;
    this.username = username;
    this.changeLog = changeLog;
  }

  public DatabaseDriver getDriver() {
    return driver;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getUsername() {
    return username;
  }

  public String getChangeLog() {
    return changeLog;
  }
}
