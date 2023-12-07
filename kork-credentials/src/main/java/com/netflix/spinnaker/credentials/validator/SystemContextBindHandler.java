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

package com.netflix.spinnaker.credentials.validator;

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.security.SystemUser;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ResolvableType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Provides a synthetic authenticated {@link SystemUser} when validating configuration property
 * bindings. This needs to be registered in a {@link ConfigurationPropertiesBindHandlerAdvisor} bean
 * to be used by Spring Boot when binding {@link ConfigurationProperties} classes.
 */
@Log4j2
public class SystemContextBindHandler extends AbstractBindHandler {
  private static final ResolvableType CREDENTIALS_DEFINITION_TYPE =
      ResolvableType.forClass(CredentialsDefinition.class);

  public SystemContextBindHandler(BindHandler parent) {
    super(parent);
  }

  @Override
  public void onFinish(
      ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
      throws Exception {
    if (CREDENTIALS_DEFINITION_TYPE.isAssignableFrom(target.getType())) {
      SecurityContext securityContext = SecurityContextHolder.getContext();
      Authentication previousAuthentication = securityContext.getAuthentication();
      securityContext.setAuthentication(SystemUser.getInstance());
      try {
        super.onFinish(name, target, context, result);
      } finally {
        securityContext.setAuthentication(previousAuthentication);
      }
    }
  }
}
