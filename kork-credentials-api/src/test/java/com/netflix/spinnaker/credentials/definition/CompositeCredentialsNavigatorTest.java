/*
 * Copyright 2023 Apple Inc.
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

package com.netflix.spinnaker.credentials.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompositeCredentialsNavigatorTest {

  CredentialsDefinitionSource<TestCredentialsDefinition> testSource1 =
      () ->
          List.of(
              new TestCredentialsDefinition("operator"),
              new TestCredentialsDefinition("user"),
              new TestCredentialsDefinition("admin"));

  CredentialsDefinitionSource<TestCredentialsDefinition> testSource2 =
      () ->
          List.of(
              new TestCredentialsDefinition("alpha"),
              new TestCredentialsDefinition("bravo"),
              new TestCredentialsDefinition("charlie"));

  CredentialsNavigator<TestCredentialsDefinition> testCredentialsNavigator =
      new CompositeCredentialsNavigator<>(
          List.of(testSource1, testSource2), TestCredentialsDefinition.class, "test");

  @ParameterizedTest
  @ValueSource(strings = {"admin", "alpha", "bravo", "charlie", "operator", "user"})
  void findByName(String name) {
    assertThat(testCredentialsNavigator.findByName(name))
        .isPresent()
        .get()
        .extracting("name")
        .isEqualTo(name);
  }

  @Test
  void getCredentialsDefinitions() {
    var expected = new ArrayList<>(testSource1.getCredentialsDefinitions());
    expected.addAll(testSource2.getCredentialsDefinitions());
    assertThat(testCredentialsNavigator.getCredentialsDefinitions())
        .containsExactlyElementsOf(expected);
  }
}
