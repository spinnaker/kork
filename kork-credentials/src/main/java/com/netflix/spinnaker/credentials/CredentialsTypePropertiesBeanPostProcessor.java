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
 *
 */

package com.netflix.spinnaker.credentials;

import com.netflix.spinnaker.credentials.definition.AbstractCredentialsLoader;
import com.netflix.spinnaker.credentials.definition.BasicCredentialsLoader;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionRepository;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinitionSource;
import com.netflix.spinnaker.credentials.definition.CredentialsParser;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import com.netflix.spinnaker.credentials.service.CompositeCredentialsDefinitionSource;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Handles registration of various credentials beans. For each registered {@code
 * CredentialsTypeProperties<C, D>} bean, an existing bean of a certain type will be used if
 * registered; otherwise, default beans are created in each of the following types. For these beans,
 * the type {@code C} is the concrete {@link Credentials} type and {@code D} is the concrete {@link
 * CredentialsDefinition} type. When {@linkplain CredentialsDefinitionRepository credentials
 * storage} is enabled, if the property {@code credentials.storage.[definition-type].enabled} is set
 * to {@code true} (where {@code [definition-type]} is the {@linkplain CredentialsType credential
 * definition type name}), then a default {@link CredentialsDefinitionSource} bean may be registered
 * which wraps the provided default credentials definition source with support for credentials
 * storage. For backward compatibility purposes, this property may also be specified as {@code
 * account.storage.[definition-name].enabled}.
 *
 * <table>
 *     <tr>
 *         <th>Bean Type</th>
 *         <th>Default Bean</th>
 *     </tr>
 *     <tr>
 *         <td>{@link CredentialsLifecycleHandler CredentialsLifecycleHandler&lt;C&gt;}</td>
 *         <td>{@link NoopCredentialsLifecycleHandler}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link CredentialsRepository CredentialsRepository&lt;C&gt;}</td>
 *         <td>{@link MapBackedCredentialsRepository} using the provided credential {@code type} and optional
 *         {@link CredentialsLifecycleHandler} bean from above</td>
 *     </tr>
 *     <tr>
 *         <td>{@link CredentialsDefinitionSource CredentialsDefinitionSource&lt;D&gt;}</td>
 *         <td>The provided {@link CredentialsDefinitionSource} which may be wrapped into a
 *         {@link CompositeCredentialsDefinitionSource} when automatic credentials storage is enabled
 *         for this credential definition type</td>
 *     </tr>
 *     <tr>
 *         <td>{@link CredentialsParser CredentialsParser&lt;D, C&gt;}</td>
 *         <td>The provided {@link CredentialsParser}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link AbstractCredentialsLoader AbstractCredentialsLoader&lt;C&gt;}</td>
 *         <td>{@link BasicCredentialsLoader} using the {@link CredentialsDefinitionSource},
 *         {@link CredentialsParser}, and {@link CredentialsRepository} beans from above, along with the
 *         provided {@code parallel} boolean flag.</td>
 *     </tr>
 * </table>
 */
@Log4j2
@Setter
public class CredentialsTypePropertiesBeanPostProcessor
    implements BeanPostProcessor, ApplicationContextAware {
  private ApplicationContext applicationContext;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    if (bean instanceof CredentialsTypeProperties<?, ?>) {
      var properties = (CredentialsTypeProperties<?, ?>) bean;
      postProcessCredentialsTypeProperties(properties);
    }
    return bean;
  }

  private <C extends Credentials, D extends CredentialsDefinition>
      void postProcessCredentialsTypeProperties(CredentialsTypeProperties<C, D> properties) {
    var loader = new CredentialsTypePropertiesBeanLoader<>(applicationContext, properties);
    log.debug("Processing CredentialsTypeProperties for type {}", properties.getType());
    loader.initializeCredentialsLoader();
  }
}
