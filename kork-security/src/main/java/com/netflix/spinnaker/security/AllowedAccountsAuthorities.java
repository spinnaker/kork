/*
 * Copyright 2020 Netflix, Inc.
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
 */

package com.netflix.spinnaker.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class AllowedAccountsAuthorities {
  public static final String PREFIX = "ALLOWED_ACCOUNT_";

  @SuppressWarnings("deprecation")
  @Nonnull
  public static Collection<String> getAllowedAccounts(@Nullable UserDetails userDetails) {
    if (userDetails == null || CollectionUtils.isEmpty(userDetails.getAuthorities())) {
      return List.of();
    }
    if (userDetails instanceof User) {
      return ((User) userDetails).getAllowedAccounts();
    }
    return filterAllowedAccountAuthorities(userDetails.getAuthorities())
        .map(AllowedAccountAuthority::getAccount)
        .sorted()
        .collect(Collectors.toList());
  }

  @Nonnull
  public static Collection<String> getAllowedAccountNames(@Nullable Authentication auth) {
    if (auth == null) {
      return List.of();
    }
    return filterAllowedAccountAuthorities(auth.getAuthorities())
        .map(AllowedAccountAuthority::getAccount)
        .sorted()
        .collect(Collectors.toList());
  }

  private static Stream<AllowedAccountAuthority> filterAllowedAccountAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    if (CollectionUtils.isEmpty(authorities)) {
      return Stream.empty();
    }
    return authorities.stream()
        .filter(AllowedAccountAuthority.class::isInstance)
        .map(AllowedAccountAuthority.class::cast);
  }

  @Nonnull
  public static Collection<AllowedAccountAuthority> buildAllowedAccounts(
      Collection<String> accounts) {
    if (accounts == null || accounts.isEmpty()) {
      return Collections.emptySet();
    }

    return accounts.stream()
        .filter(StringUtils::hasLength)
        .map(AllowedAccountAuthority::new)
        .collect(Collectors.toSet());
  }
}
