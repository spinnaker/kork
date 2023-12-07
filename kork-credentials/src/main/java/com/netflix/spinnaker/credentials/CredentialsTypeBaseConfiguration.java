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

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Provides a backward compatible way to register a {@link CredentialsTypeProperties} bean and its
 * associated credentials beans. It is recommended to directly use {@link CredentialsTypeProperties}
 * and register it as a bean rather than creating beans of this class.
 *
 * @see CredentialsTypePropertiesBeanPostProcessor
 * @deprecated use {@link CredentialsTypeProperties} directly
 */
@RequiredArgsConstructor
@Log4j2
@Deprecated(since = "2023-10-13")
public class CredentialsTypeBaseConfiguration<
        T extends Credentials, U extends CredentialsDefinition>
    implements InitializingBean, ApplicationContextAware {
  private final CredentialsTypeProperties<T, U> properties;
  @Nullable @Setter private ApplicationContext applicationContext;

  public CredentialsTypeBaseConfiguration(
      @Nullable ApplicationContext applicationContext, CredentialsTypeProperties<T, U> properties) {
    this.properties = properties;
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() {
    var context =
        Objects.requireNonNull(applicationContext, "No ApplicationContext value was provided");
    var loader = new CredentialsTypePropertiesBeanLoader<>(context, properties);
    loader.initializeCredentialsLoader();
  }
}
