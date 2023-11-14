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
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;

/** ArtifactStore allows for different types of artifact storage to be used during runtime */
public class ArtifactStore implements ArtifactStoreGetter, ArtifactStoreStorer {
  /** ensures the singleton has only been set once */
  private static final AtomicBoolean singletonSet = new AtomicBoolean(false);

  @Getter private static ArtifactStore instance = null;

  private final ArtifactStoreGetter artifactStoreGetter;

  private final ArtifactStoreStorer artifactStoreStorer;

  public ArtifactStore(
      ArtifactStoreGetter artifactStoreGetter, ArtifactStoreStorer artifactStoreStorer) {
    this.artifactStoreGetter = artifactStoreGetter;
    this.artifactStoreStorer = artifactStoreStorer;
  }

  /** Store an artifact in the artifact store */
  public Artifact store(Artifact artifact) {
    return artifactStoreStorer.store(artifact);
  }

  /**
   * get is used to return an artifact with some id, while also decorating that artifact with any
   * necessary fields needed which should be then be returned by the artifact store.
   */
  public Artifact get(ArtifactReferenceURI uri, ArtifactDecorator... decorators) {
    return artifactStoreGetter.get(uri, decorators);
  }

  public static void setInstance(ArtifactStore storage) {
    if (!singletonSet.compareAndSet(false, true)) {
      throw new IllegalStateException("Multiple attempts at setting ArtifactStore's singleton");
    }

    ArtifactStore.instance = storage;
  }

  /**
   * Meant to be called during spring context cleanup, e.g. via a bean's destroyMethod, or for
   * explicit cleanup in a non-spring test.
   */
  public static void clearInstance() {
    ArtifactStore.instance = null;
    singletonSet.set(false);
  }

  public boolean isArtifactURI(String value) {
    return value.startsWith(ArtifactStoreURIBuilder.uriScheme + "://");
  }
}
