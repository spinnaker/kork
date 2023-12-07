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

package com.netflix.spinnaker.credentials.definition;

import com.netflix.spinnaker.credentials.Credentials;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Wrapper for a {@link CredentialsParser} instance that prevents exceptions thrown during parsing
 * from propagating to the caller. Invalid credentials are instead logged about and counted in
 * metrics.
 */
@Log4j2
@RequiredArgsConstructor
public class SafeCredentialsParser<T extends CredentialsDefinition, U extends Credentials>
    implements CredentialsParser<T, U> {
  private final Counter parseErrors = Metrics.counter("credentials.parse.error");
  private final CredentialsParser<T, U> delegate;

  @Nullable
  @Override
  public U parse(T definition) {
    try {
      return delegate.parse(definition);
    } catch (VirtualMachineError | ThreadDeath e) {
      // these exceptions are unrecoverable and unremarkable (nothing to track here that JVM metrics
      // don't already)
      throw e;
    } catch (Throwable t) {
      parseErrors.increment();
      log.warn("Unable to parse credentials definition with name '{}'", definition.getName(), t);
      return null;
    }
  }
}
