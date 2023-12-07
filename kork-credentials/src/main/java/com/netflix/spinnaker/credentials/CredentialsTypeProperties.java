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

import com.netflix.spinnaker.credentials.definition.AbstractCredentialsLoader;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.credentials.definition.CredentialsParser;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * CredentialsTypeProperties is a convenient way to configure {@link CredentialsRepository}, {@link
 * CredentialsDefinitionSource}, and {@link AbstractCredentialsLoader} for various credentials
 * classes. It will try to reuse any {@link CredentialsRepository}, {@link
 * CredentialsDefinitionSource}, {@link CredentialsParser}, {@link CredentialsLifecycleHandler}, or
 * {@link AbstractCredentialsLoader} that may have been added as a bean and create default
 * implementations otherwise.
 *
 * <h2>Example Usage</h2>
 *
 * Suppose we have credentials classes defined for some credentials type named {@code cake} with its
 * {@link CredentialsDefinition} class named {@code CakeDefinition}, its {@link Credentials} class
 * named {@code CakeCredentials}, and a Spring Boot {@link ConfigurationProperties} bean for the
 * configuration properties class {@code BakeryProperties} containing a property defined as {@code
 * List<CakeDefinition> accounts} (i.e., a {@link CredentialsDefinitionSource} of {@code
 * BakeryProperties::getAccounts}), then the most basic way to configure this cake credential type
 * would work like the following code snippet from a {@link Configuration} class:
 *
 * <pre>
 * &#64;Bean
 * CredentialsTypeProperties&lt;CakeCredentials, CakeDefinition&gt;
 *     cakeCredentialsTypeProperties(
 *         BakeryProperties properties,
 *         ...) {
 *   return CredentialsTypeProperties.builder(
 *           CakeCredentials.class, CakeDefinition.class)
 *       .type("cake")
 *       .defaultCredentialsSource(properties::getAccounts)
 *       .credentialsParser(
 *           definition -> {
 *               CakeCredentials credentials;
 *               // ... logic to convert definition into credentials
 *               // (includes any other injected beans)
 *               return credentials;
 *           })
 *       .build();
 * }
 * </pre>
 *
 * If the property {@code credentials.storage.cake.enabled} is set to {@code true}, then the above
 * example will handle the setup for credentials storage support for cake accounts.
 */
@Getter
@ToString(of = {"type", "credentialsClass", "credentialsDefinitionClass"})
public class CredentialsTypeProperties<T extends Credentials, U extends CredentialsDefinition> {
  /** Specifies the credentials type name this is for. */
  private final String type;

  /** Specifies the type of credentials being configured. */
  private final Class<T> credentialsClass;

  /** Specifies the type of credential definitions being configured. */
  private final Class<U> credentialsDefinitionClass;

  /**
   * Specifies a default fallback source for credentials definitions if no {@link
   * CredentialsDefinitionSource} bean is otherwise registered for this credentials definition type.
   */
  private final CredentialsDefinitionSource<U> defaultCredentialsSource;

  /**
   * Specifies the parser to use to transform credentials definitions into credentials. If left
   * null, this must be configured as a bean elsewhere.
   */
  @Nullable private final CredentialsParser<U, T> credentialsParser;

  /** Enables parallel credentials parsing. */
  private final boolean parallel;

  @Builder
  public CredentialsTypeProperties(
      @Nonnull String type,
      @Nonnull Class<T> credentialsClass,
      @Nonnull Class<U> credentialsDefinitionClass,
      @Nullable CredentialsDefinitionSource<U> defaultCredentialsSource,
      @Nullable CredentialsParser<U, T> credentialsParser,
      boolean parallel) {
    Assert.hasText(type, "No credential type provided");
    this.type = type;
    this.credentialsClass = credentialsClass;
    this.credentialsDefinitionClass = credentialsDefinitionClass;
    this.defaultCredentialsSource =
        defaultCredentialsSource != null ? defaultCredentialsSource : List::of;
    this.credentialsParser = credentialsParser;
    this.parallel = parallel;
  }

  public static <T extends Credentials, U extends CredentialsDefinition>
      CredentialsTypePropertiesBuilder<T, U> builder() {
    return new CredentialsTypePropertiesBuilder<>();
  }

  public static <T extends Credentials, U extends CredentialsDefinition>
      CredentialsTypePropertiesBuilder<T, U> builder(
          Class<T> credentialsClass, Class<U> credentialsDefinitionClass) {
    return new CredentialsTypePropertiesBuilder<T, U>()
        .credentialsClass(credentialsClass)
        .credentialsDefinitionClass(credentialsDefinitionClass);
  }
}
