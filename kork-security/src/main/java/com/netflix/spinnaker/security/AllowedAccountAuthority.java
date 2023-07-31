/*
 * Copyright 2023 Apple Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.security;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.Locale;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

/**
 * Represents an authorization to use an account. Allowed account authorities are based on an older
 * security mechanism used by Spinnaker before other account types besides cloud providers and
 * permissions were added.
 */
@Getter
@EqualsAndHashCode(of = "account")
@NonnullByDefault
public class AllowedAccountAuthority implements GrantedAuthority {
  private final String account;
  private final String authority;

  public AllowedAccountAuthority(String account) {
    this.account = account.toLowerCase(Locale.ROOT);
    authority = AllowedAccountsAuthorities.PREFIX + this.account;
  }
}
