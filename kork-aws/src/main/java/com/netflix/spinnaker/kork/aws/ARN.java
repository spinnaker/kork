/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.kork.aws;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util for easy parsing of an Amazon Resource Names (ARNS)
 * https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
 */
public class ARN {

  // We're currently using version 1.11.1009 of the aws java sdk, so the source
  // of truth for available partitions is
  // https://github.com/aws/aws-sdk-java/blob/1.11.1009/aws-java-sdk-core/src/main/resources/com/amazonaws/partitions/endpoints.json,
  // or more specifically:
  //
  // $ cat ./aws-java-sdk-core/src/main/resources/com/amazonaws/partitions/endpoints.json | jq -r
  // '.partitions[] | .partition'
  // aws
  // aws-cn
  // aws-us-gov
  // aws-iso
  // aws-iso-b
  static final Pattern PATTERN =
      Pattern.compile("arn:aws(?:-cn|-us-gov|-iso|-iso-b)?:.*:(.*):(\\d+):(.*)");

  private String arn;
  private String region;
  private String name;
  private String account;

  public ARN(String arn) {
    this.arn = arn;

    Matcher arnMatcher = PATTERN.matcher(arn);
    if (!arnMatcher.matches()) {
      throw new IllegalArgumentException(arn + " is not a valid ARN");
    }

    this.region = arnMatcher.group(1);
    this.account = arnMatcher.group(2);
    this.name = arnMatcher.group(3);
  }

  public String getArn() {
    return arn;
  }

  public String getRegion() {
    return region;
  }

  public String getName() {
    return name;
  }

  public String getAccount() {
    return account;
  }

  @Override
  public String toString() {
    return "ARN{" + "arn='" + arn + '\'' + '}';
  }
}
