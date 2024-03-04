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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Holder for additional {@link ConstraintViolation} metadata when performing constraint validation.
 * A request-scoped bean of this class should be registered for use with an exception handler.
 */
@NonnullByDefault
public interface ConstraintViolationContext {
  void addViolation(ConstraintViolation violation);

  default Optional<ConstraintViolation> removeMatchingViolation(
      Predicate<ConstraintViolation> predicate) {
    Iterator<ConstraintViolation> iterator = getConstraintViolations().iterator();
    while (iterator.hasNext()) {
      ConstraintViolation violation = iterator.next();
      if (predicate.test(violation)) {
        iterator.remove();
        return Optional.of(violation);
      }
    }
    return Optional.empty();
  }

  List<ConstraintViolation> getConstraintViolations();
}
