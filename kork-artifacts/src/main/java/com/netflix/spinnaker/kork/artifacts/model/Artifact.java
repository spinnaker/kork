/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.artifacts.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.netflix.spinnaker.kork.annotations.NullableByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@NullableByDefault
@JsonDeserialize(builder = Artifact.ArtifactBuilder.class)
public final class Artifact {
  private String type;
  private boolean customKind;
  private String name;
  private String version;
  private String location;
  private String reference;
  @Nonnull private Map<String, Object> metadata;
  private String artifactAccount;
  private String provenance;
  private String uuid;

  @Builder(toBuilder = true)
  @ParametersAreNullableByDefault
  private Artifact(
      String type,
      boolean customKind,
      String name,
      String version,
      String location,
      String reference,
      Map<String, Object> metadata,
      String artifactAccount,
      String provenance,
      String uuid) {
    this.type = type;
    this.customKind = customKind;
    this.name = name;
    this.version = version;
    this.location = location;
    this.reference = reference;
    this.metadata = Optional.ofNullable(metadata).orElseGet(HashMap::new);
    this.artifactAccount = artifactAccount;
    this.provenance = provenance;
    this.uuid = uuid;
  }

  // All setters for this class are deprecated. In general, artifacts are passed around the pipeline
  // context and are liberally serialized and deserialized, which makes mutating them fraught with
  // peril (because you might or might not actually be operating on a copy of the artifact you
  // actually want to mutate). In order to remove this cause of subtle bugs, this class will soon
  // become immutable and these setters will be removed.

  // The encouraged pattern for adding a field to an artifact is:
  // Artifact newArtifact = artifact.toBuilder().artifactAccount("my-account").build()

  @Deprecated
  public void setType(String type) {
    this.type = type;
  }

  @Deprecated
  public void setCustomKind(boolean customKind) {
    this.customKind = customKind;
  }

  @Deprecated
  public void setName(String name) {
    this.name = name;
  }

  @Deprecated
  public void setVersion(String version) {
    this.version = version;
  }

  @Deprecated
  public void setLocation(String location) {
    this.location = location;
  }

  @Deprecated
  public void setReference(String reference) {
    this.reference = reference;
  }

  @Deprecated
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = Optional.ofNullable(metadata).orElseGet(HashMap::new);
  }

  @Deprecated
  public void setArtifactAccount(String artifactAccount) {
    this.artifactAccount = artifactAccount;
  }

  @Deprecated
  public void setProvenance(String provenance) {
    this.provenance = provenance;
  }

  @Deprecated
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @JsonIgnoreProperties("kind")
  @JsonPOJOBuilder(withPrefix = "")
  public static class ArtifactBuilder {
    @Nonnull private Map<String, Object> metadata = new HashMap<>();

    // Add extra, unknown data to the metadata map:
    @JsonAnySetter
    public void putMetadata(String key, Object value) {
      metadata.put(key, value);
    }
  }
}
