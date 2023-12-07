/*
 * Copyright 2020 Armory
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
 *
 */

package com.netflix.spinnaker.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.netflix.spinnaker.credentials.definition.*;
import java.util.List;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class CredentialsTypeBaseConfigurationTest {
  private static final String CREDENTIALS_TYPE = "test";
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(CredentialsTypePropertiesBeanPostProcessor.class)
          .withUserConfiguration(TestConfig.class);

  static class TestConfig {
    @Bean
    CredentialsTypeProperties<TestCredentials, TestAccount> testProperties(
        Environment environment) {
      boolean parallel = environment.getProperty("credentials.parallel", Boolean.class, false);
      return CredentialsTypeProperties.builder(TestCredentials.class, TestAccount.class)
          .type(CREDENTIALS_TYPE)
          .defaultCredentialsSource(
              () -> List.of(new TestAccount("account1"), new TestAccount("account2")))
          .credentialsParser(TestCredentials::new)
          .parallel(parallel)
          .build();
    }
  }

  @Test
  public void testCreateCredentials() {
    contextRunner.run(
        context ->
            assertThat(context)
                .hasSingleBean(CredentialsRepository.class)
                .getBean(CredentialsRepository.class)
                .extracting(repository -> repository.getOne("account1"))
                .isNotNull());
  }

  @Test
  public void testOverrideParser() {
    contextRunner
        .withBean("customParser", TestCustomParser.class, TestCustomParser::new)
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(CredentialsRepository.class)
                    .getBean(CredentialsRepository.class)
                    .extracting(repository -> repository.getOne("account1"))
                    .isNotNull()
                    .asInstanceOf(type(TestCredentials.class))
                    .returns("my-value", from(TestCredentials::getProperty)));
  }

  @Test
  public void testOverrideLifecycleHandler() {
    TestLifecycleHandler handler = mock(TestLifecycleHandler.class);
    contextRunner
        .withBean(TestLifecycleHandler.class, () -> handler)
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(CredentialsRepository.class)
                    .getBean(CredentialsRepository.class)
                    .extracting(repository -> repository.getOne("account1"))
                    .isNotNull()
                    .asInstanceOf(type(TestCredentials.class))
                    .satisfies(
                        testCredentials -> verify(handler).credentialsAdded(testCredentials)));
  }

  @Test
  public void testOverrideSource() {
    contextRunner
        .withBean(
            TestSource.class, List.of(new TestAccount("account3"), new TestAccount("account4")))
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(CredentialsRepository.class)
                    .getBean(CredentialsRepository.class)
                    .satisfies(
                        repository -> {
                          assertThat(repository).isNotNull();
                          assertThat(repository.getOne("account3")).isNotNull();
                          assertThat(repository.getOne("account1")).isNull();
                        }));
  }

  @Test
  public void testOverrideCredentialsRepository() {
    TestCredentialsRepository repository =
        new TestCredentialsRepository(CREDENTIALS_TYPE, new NoopCredentialsLifecycleHandler<>());
    contextRunner
        .withBean(TestCredentialsRepository.class, () -> repository)
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(CredentialsRepository.class)
                    .getBean(CredentialsRepository.class)
                    .satisfies(repo -> assertThat(repo).isSameAs(repository)));
  }

  @Test
  void testCredentialsStorageEnabled() {
    var repository = mock(CredentialsDefinitionRepository.class);
    given(repository.listByType("test")).willReturn(List.of(new TestAccount("account3")));
    contextRunner
        .withPropertyValues("credentials.storage.test.enabled=true")
        .withBean(CredentialsDefinitionRepository.class, () -> repository)
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(CredentialsRepository.class)
                    .getBean(CredentialsRepository.class)
                    .extracting(CredentialsRepository::getAll)
                    .asInstanceOf(iterable(Credentials.class))
                    .extracting(Credentials::getName)
                    .containsExactlyInAnyOrder("account1", "account2", "account3"));
  }

  @Test
  public void testParallel() {
    contextRunner
        .withPropertyValues("credentials.parallel=true")
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(BasicCredentialsLoader.class)
                    .getBean(BasicCredentialsLoader.class)
                    .returns(true, from(BasicCredentialsLoader::isParallel)));
  }

  private static class TestCredentialsRepository
      extends MapBackedCredentialsRepository<TestCredentials> {

    public TestCredentialsRepository(
        String type, CredentialsLifecycleHandler<TestCredentials> eventHandler) {
      super(type, eventHandler);
    }
  }

  @Data
  @CredentialsType(CREDENTIALS_TYPE)
  private static class TestAccount implements CredentialsDefinition {
    private final String name;
  }

  @Data
  private static class TestSource implements CredentialsDefinitionSource<TestAccount> {
    private final List<TestAccount> credentialsDefinitions;
  }

  private static class TestCustomParser implements CredentialsParser<TestAccount, TestCredentials> {
    @Override
    public TestCredentials parse(TestAccount account) {
      TestCredentials creds = new TestCredentials(account);
      creds.setProperty("my-value");
      return creds;
    }
  }

  private static class TestLifecycleHandler
      extends NoopCredentialsLifecycleHandler<TestCredentials> {}

  @Data
  private static class TestCredentials implements Credentials {
    private final TestAccount testAccount;
    private String property;

    public String getName() {
      return testAccount.getName();
    }

    @Override
    public String getType() {
      return CREDENTIALS_TYPE;
    }
  }
}
