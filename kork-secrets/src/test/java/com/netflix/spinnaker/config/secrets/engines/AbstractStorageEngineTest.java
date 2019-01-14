package com.netflix.spinnaker.config.secrets.engines;

import com.netflix.spinnaker.config.secrets.EncryptedSecret;
import com.netflix.spinnaker.config.secrets.SecretDecryptionException;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

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
    assertEquals("value", engine.getParsedValue("a/b", "test"));
    assertEquals("othervalue", engine.getParsedValue("a/b", "a.b"));
  }

}
