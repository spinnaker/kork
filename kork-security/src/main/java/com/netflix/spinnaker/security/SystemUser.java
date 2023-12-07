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

package com.netflix.spinnaker.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/** Synthetic system user for use in service bootstrapping contexts. */
public class SystemUser extends AbstractAuthenticationToken {
  public static final String NAME = "SYSTEM";

  private static final SystemUser INSTANCE = new SystemUser();

  private SystemUser() {
    super(List.of(SpinnakerAuthorities.ADMIN_AUTHORITY, SpinnakerAuthorities.ANONYMOUS_AUTHORITY));
    setAuthenticated(true);
  }

  @Override
  public Object getPrincipal() {
    return NAME;
  }

  @Override
  public Object getCredentials() {
    return NAME;
  }

  @Override
  public String getName() {
    return NAME;
  }

  public static SystemUser getInstance() {
    return INSTANCE;
  }
}
