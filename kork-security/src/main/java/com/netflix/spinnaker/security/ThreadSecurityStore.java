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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.MDC;

/**
 * Thin adapter around a ThreadLocal used to store request-scoped security data.
 *
 * <p>This class also forwards writes to MDC so that these values can be available for logging as
 * well.
 */
public class ThreadSecurityStore {

  private static final ThreadLocal<Map<String, String>> STORE = new ThreadLocal<>();

  private ThreadSecurityStore() {
    // Don't allow creating new instances.
  }

  /** Put a value, or if the value is null, remove the key from the store. */
  public static void putOrRemove(@Nonnull String key, @Nullable String value) {
    Map<String, String> m = STORE.get();
    if (m == null) {
      m = new HashMap<>();
      STORE.set(m);
    }

    if (value == null) {
      m.remove(key);
      MDC.remove(key);
    } else {
      m.put(key, value);
      MDC.put(key, value);
    }
  }

  @Nullable
  public static String get(@Nonnull String key) {
    Map<String, String> m = STORE.get();
    if (m == null) {
      return null;
    }
    return m.get(key);
  }

  @Nonnull
  public static Map<String, String> getCopy() {
    Map<String, String> m = STORE.get();
    if (m == null) {
      return new HashMap<>();
    }
    return new HashMap<>(m);
  }

  public static void set(@Nonnull Map<String, String> map) {
    STORE.set(map);
    MDC.setContextMap(map);
  }

  public static void clear() {
    STORE.remove();
    MDC.clear();
    try {
      // force clear to avoid the potential for a memory leak if log4j is being used
      Class<?> log4jMDC = Class.forName("org.apache.log4j.MDC");
      log4jMDC.getDeclaredMethod("clear").invoke(null);
    } catch (Exception ignored) {
    }
  }
}
