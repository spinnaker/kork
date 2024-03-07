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

package com.netflix.spinnaker.kork.artifacts.artifactstore.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.netflix.spinnaker.kork.artifacts.ArtifactTypes;
import com.netflix.spinnaker.kork.artifacts.artifactstore.ArtifactStoreURISHA256Builder;
import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class S3ArtifactStorerTest {
  @Test
  public void testInvalidEmbeddedBase64StillSucceeds() {
    S3Client client = mock(S3Client.class);
    AuthenticatedRequest.setApplication("my-application");
    S3ArtifactStore artifactStore =
        new S3ArtifactStore(client, null, "my-bucket", new ArtifactStoreURISHA256Builder(), null);
    String expectedReference = "${ #nonbase64spel() }";
    Artifact artifact =
        artifactStore.store(
            Artifact.builder()
                .type(ArtifactTypes.EMBEDDED_BASE64.getMimeType())
                .reference(expectedReference)
                .build());
    assertEquals(expectedReference, artifact.getReference());
    assertEquals(ArtifactTypes.EMBEDDED_BASE64.getMimeType(), artifact.getType());
  }
}
