/*
 * Copyright 2015 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.netflix.spinnaker.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticatedRequest {
  public static final String SPINNAKER_USER = "X-SPINNAKER-USER";
  public static final String SPINNAKER_ACCOUNTS = "X-SPINNAKER-ACCOUNTS";

  public static <V> Callable<V> propagate(Callable<V> closure) {
    return propagate(closure, true, principal());
  }

  public static <V> Callable<V> propagate(Callable<V> closure, boolean restoreOriginalContext) {
    return propagate(closure, restoreOriginalContext, principal());
  }

  public static <V> Callable<V> propagate(Callable<V> closure, Object principal) {
    return propagate(closure, true, principal);
  }

  /**
   * Ensure an appropriate MDC context is available when {@code closure} is executed.
   */
  public static <V> Callable<V> propagate(Callable<V> closure, boolean restoreOriginalContext, Object principal) {
    String spinnakerUser = getSpinnakerUser(principal).orElse(null);
    if (spinnakerUser == null) {
      return () -> {
        MDC.remove(SPINNAKER_USER);
        MDC.remove(SPINNAKER_ACCOUNTS);
        return closure.call();
      };
    }

    String spinnakerAccounts = getSpinnakerAccounts(principal).orElse(null);

    return () -> {
      String originalSpinnakerUser = MDC.get(SPINNAKER_USER);
      String originalSpinnakerAccounts = MDC.get(SPINNAKER_ACCOUNTS);
      try {
        MDC.put(SPINNAKER_USER, spinnakerUser);
        if (spinnakerAccounts != null) {
          MDC.put(SPINNAKER_ACCOUNTS, spinnakerAccounts);
        }
        return closure.call();
      } finally {
        MDC.clear();

        try {
          // force clear to avoid the potential for a memory leak if log4j is being used
          Class log4jMDC = Class.forName("org.apache.log4j.MDC");
          log4jMDC.getDeclaredMethod("clear").invoke(null);
        } catch (Exception ignored) { }

        if (originalSpinnakerUser != null && restoreOriginalContext) {
          MDC.put(SPINNAKER_USER, originalSpinnakerUser);
        }

        if (originalSpinnakerAccounts != null && restoreOriginalContext) {
          MDC.put(SPINNAKER_ACCOUNTS, originalSpinnakerAccounts);
        }
      }
    };
  }

  public static Map<String, Optional<String>> getAuthenticationHeaders() {
    Map<String, Optional<String>> headers = new HashMap<>();
    headers.put(SPINNAKER_USER, getSpinnakerUser());
    headers.put(SPINNAKER_ACCOUNTS, getSpinnakerAccounts());
    return headers;
  }

  public static Optional<String> getSpinnakerUser() {
    return getSpinnakerUser(principal());
  }

  public static Optional<String> getSpinnakerUser(Object principal) {
    Object spinnakerUser = MDC.get(SPINNAKER_USER);

    if (principal != null && principal instanceof User) {
      spinnakerUser = ((User) principal).getUsername();
    }

    return Optional.ofNullable((String) spinnakerUser);
  }

  public static Optional<String> getSpinnakerAccounts() {
    return getSpinnakerAccounts(principal());
  }

  public static Optional<String> getSpinnakerAccounts(Object principal) {
    Object spinnakerAccounts = MDC.get(SPINNAKER_ACCOUNTS);

    if (principal != null && principal instanceof User && !((User) principal).getAllowedAccounts().isEmpty()) {
      spinnakerAccounts = String.join(",", ((User) principal).getAllowedAccounts());
    }

    return Optional.ofNullable((String) spinnakerAccounts);
  }

  /**
   * @return the Spring Security principal or null if there is no authority.
   */
  private static Object principal() {
    return Optional
      .ofNullable(SecurityContextHolder.getContext().getAuthentication())
      .map(Authentication::getPrincipal)
      .orElse(null);
  }
}
