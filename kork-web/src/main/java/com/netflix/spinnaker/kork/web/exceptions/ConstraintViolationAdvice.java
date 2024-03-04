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

package com.netflix.spinnaker.kork.web.exceptions;

import com.netflix.spinnaker.kork.api.exceptions.ConstraintViolationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
public class ConstraintViolationAdvice {
  private final ConstraintViolationContext context;

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ConstraintViolationResponse> handleConstraintViolationException(
      ConstraintViolationException e) {
    ConstraintViolationResponse response = new ConstraintViolationResponse();
    List<ConstraintViolationError> errors = combine(e.getConstraintViolations(), context);
    response.setErrors(errors);
    return ResponseEntity.badRequest().body(response);
  }

  private static List<ConstraintViolationError> combine(
      Set<ConstraintViolation<?>> constraintViolations, ConstraintViolationContext context) {
    var errors =
        constraintViolations.stream()
            .map(
                constraintViolation ->
                    context
                        .removeMatchingViolation(
                            violation -> violationsMatch(constraintViolation, violation))
                        .map(
                            violation ->
                                ConstraintViolationError.builder()
                                    .message(violation.getMessage())
                                    .errorCode(violation.getErrorCode())
                                    .path(constraintViolation.getPropertyPath().toString())
                                    .additionalAttributes(violation.getAdditionalAttributes())
                                    .build())
                        .orElseGet(() -> translate(constraintViolation)))
            .collect(Collectors.toCollection(ArrayList::new));
    context.getConstraintViolations().stream()
        .map(ConstraintViolationAdvice::translate)
        .forEach(errors::add);
    return errors;
  }

  private static boolean violationsMatch(
      ConstraintViolation<?> constraintViolation,
      com.netflix.spinnaker.kork.api.exceptions.ConstraintViolation violation) {
    return constraintViolation.getLeafBean() == violation.getValidatedObject()
        && constraintViolation.getMessageTemplate().equals(violation.getErrorCode());
  }

  private static ConstraintViolationError translate(ConstraintViolation<?> violation) {
    return ConstraintViolationError.builder()
        .message(violation.getMessage())
        .errorCode(violation.getMessageTemplate())
        .path(violation.getPropertyPath().toString())
        .build();
  }

  private static ConstraintViolationError translate(
      com.netflix.spinnaker.kork.api.exceptions.ConstraintViolation violation) {
    return ConstraintViolationError.builder()
        .message(violation.getMessage())
        .errorCode(violation.getErrorCode())
        .path(violation.getPath())
        .additionalAttributes(violation.getAdditionalAttributes())
        .build();
  }
}
