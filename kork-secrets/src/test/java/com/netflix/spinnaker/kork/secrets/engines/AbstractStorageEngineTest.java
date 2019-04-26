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

import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.SecretDecryptionException;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class AbstractStorageEngineTest {
  AbstractStorageSecretEngine engine;

  @Before
  public void init() {
    engine = new AbstractStorageSecretEngine() {
      @Override
      protected InputStream downloadRemoteFile(EncryptedSecret encryptedSecret) {
        return null;
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
  public void canParseYaml() throws SecretDecryptionException {
    ByteArrayInputStream bis = readStream("test: value\na:\n  b: othervalue\nc:\n  - d\n  - e");
    engine.yamlParser = new Yaml();
    engine.parseAsYaml("a/b", bis);
    assertTrue(Arrays.equals("value".getBytes(), engine.getParsedValue("a/b", "test")));
    assertTrue(Arrays.equals("othervalue".getBytes(), engine.getParsedValue("a/b", "a.b")));
  }

}
