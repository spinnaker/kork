/*
 * Copyright 2020 Google, LLC
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

package com.netflix.spinnaker.kork.artifacts.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
final class ExpectedArtifactTest {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    // This avoids needing to write out all null values in our expected JSON and is how the
    // objectMapper in orca/clouddriver are configured.
    objectMapper.setSerializationInclusion(Include.NON_NULL);
  }

  private static final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

  @Test
  void deserializeAllFields() throws IOException {
    ExpectedArtifact result =
        objectMapper.readValue(fullExpectedArtifactJson(), ExpectedArtifact.class);
    ExpectedArtifact expected = fullExpectedArtifact();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void serializeAllFields() throws IOException {
    String result = objectMapper.writeValueAsString(fullExpectedArtifact());
    String expected = fullExpectedArtifactJson();

    // Compare the parsed trees of the two results, which is agnostic to key order
    AssertionsForClassTypes.assertThat(objectMapper.readTree(result))
        .isEqualTo(objectMapper.readTree(expected));
  }

  @Test
  void roundTripSerialization() throws IOException {
    ExpectedArtifact artifact = fullExpectedArtifact();
    String json = objectMapper.writeValueAsString(artifact);
    ExpectedArtifact deserializedArtifact = objectMapper.readValue(json, ExpectedArtifact.class);
    assertThat(deserializedArtifact).isEqualTo(artifact);
  }

  private String fullExpectedArtifactJson() {
    return jsonFactory
        .objectNode()
        .put("id", "test")
        .put("usePriorArtifact", true)
        .put("useDefaultArtifact", false)
        // We're using valueToTree rather than writing out the serialization of the Artifact class
        // on the assumption that the serialization of Artifact is separately tested.
        .<ObjectNode>set(
            "matchArtifact",
            objectMapper.valueToTree(Artifact.builder().type("gcs/object").build()))
        .<ObjectNode>set(
            "boundArtifact",
            objectMapper.valueToTree(
                Artifact.builder().type("gcs/object").name("my-artifact").build()))
        .toString();
  }

  private ExpectedArtifact fullExpectedArtifact() {
    return ExpectedArtifact.builder()
        .id("test")
        .usePriorArtifact(true)
        .useDefaultArtifact(false)
        .matchArtifact(Artifact.builder().type("gcs/object").build())
        .boundArtifact(Artifact.builder().type("gcs/object").name("my-artifact").build())
        .build();
  }
}
