package com.netflix.spinnaker.kork.spring;

import spock.lang.Specification;
import spock.lang.Subject;
import java.nio.file.Paths;

class PluginLoaderSpec extends Specification {

  File pluginConfig;
  FileInputStream inputStream;

  @Subject
  PluginLoader subject = new PluginLoader()

  def "should return enabled plugin paths"() {
    given:
    pluginConfig = new File("./src/test/resources/pluginConfigurationExample.yml")
    inputStream = new FileInputStream(pluginConfig)

    expect:
    subject.getEnabledJarPaths() == expected

    where:
    feature     | criteria                                      || expected
    null | null | [Paths.get("/opt/spinnaker/plugins/stage-plugin-0.0.1-SNAPSHOT.jar"), Paths.get("/opt/spinnaker/plugins/stage-plugin-0.0.1-SNAPSHOT.jar")]
  }

}
