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

package com.netflix.spinnaker.kork.api.exceptions;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Default implementation of {@link ConstraintViolationContext}. This should be registered as a
 * request-scoped bean so that a request exception handler can use it.
 */
@Getter
@NonnullByDefault
public class DefaultConstraintViolationContext implements ConstraintViolationContext {
  private final List<ConstraintViolation> constraintViolations = new ArrayList<>();

  @Override
  public void addViolation(ConstraintViolation violation) {
    constraintViolations.add(violation);
  }
}
