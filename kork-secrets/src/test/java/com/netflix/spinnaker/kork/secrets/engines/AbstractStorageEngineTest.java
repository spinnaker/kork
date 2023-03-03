/*
 * Copyright 2019 Armory, Inc.
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

package com.netflix.spinnaker.kork.secrets.engines;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.yaml.snakeyaml.Yaml;

public class AbstractStorageEngineTest {
  Map<String, Object> secretMap =
      Map.of(
          "test", "value",
          "a", Map.of("b", "othervalue"),
          "c", List.of("d", "e"));
  AbstractStorageSecretEngine engine;

  @Before
  public void init() {
    String secret = new Yaml().dump(secretMap);
    engine =
        new AbstractStorageSecretEngine(new ConcurrentMapCache("test")) {
          @Override
          protected InputStream downloadRemoteFile(CacheKey cacheKey) throws IOException {
            return readStream(secret);
          }

          @Override
          public String identifier() {
            return "test";
          }
        };
  }

  protected ByteArrayInputStream readStream(String value) {
    return new ByteArrayInputStream(value.getBytes());
  }

  @Test
  public void canParseYaml() throws SecretDecryptionException, IOException {
    AbstractStorageSecretEngine.CacheKey cacheKey =
        new AbstractStorageSecretEngine.CacheKey("bucket", "region", "test-file");

    assertArrayEquals(
        "value".getBytes(StandardCharsets.UTF_8), engine.getSecretValue(cacheKey, "test"));
    assertArrayEquals(
        "othervalue".getBytes(StandardCharsets.UTF_8), engine.getSecretValue(cacheKey, "a.b"));
    assertArrayEquals("d".getBytes(StandardCharsets.UTF_8), engine.getSecretValue(cacheKey, "c.0"));
    assertArrayEquals("e".getBytes(StandardCharsets.UTF_8), engine.getSecretValue(cacheKey, "c.1"));

    assertThrows(SecretDecryptionException.class, () -> engine.getSecretValue(cacheKey, "a"));
    assertThrows(SecretDecryptionException.class, () -> engine.getSecretValue(cacheKey, "b"));
    assertThrows(SecretDecryptionException.class, () -> engine.getSecretValue(cacheKey, "c.2"));
  }
}
