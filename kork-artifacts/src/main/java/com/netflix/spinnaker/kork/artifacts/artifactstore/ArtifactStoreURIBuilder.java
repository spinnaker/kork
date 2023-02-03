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
package com.netflix.spinnaker.kork.artifacts.artifactstore;

import com.netflix.spinnaker.kork.artifacts.model.Artifact;

public abstract class ArtifactStoreURIBuilder {
  /**
   * uriScheme is used as an HTTP scheme to let us further distinguish a String that is a URI to an
   * artifact. This is helpful in determining what is an artifact since sometimes we are only given
   * a string rather than a full artifact.
   */
  public static final String uriScheme = "ref";

  public abstract String buildArtifactURI(String context, Artifact artifact);

  /**
   * buildRawURI is used when you have the raw path and context. This method just simply return the
   * properly formatted URI
   */
  public abstract String buildRawURI(String context, String raw);
}
