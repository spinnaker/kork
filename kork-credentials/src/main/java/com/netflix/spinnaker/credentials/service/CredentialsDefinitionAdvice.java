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

package com.netflix.spinnaker.credentials.service;

import com.netflix.spinnaker.credentials.definition.error.CredentialsDefinitionRepositoryException;
import com.netflix.spinnaker.credentials.definition.error.DuplicateCredentialsDefinitionException;
import com.netflix.spinnaker.credentials.definition.error.InvalidCredentialsDefinitionException;
import com.netflix.spinnaker.credentials.definition.error.NoMatchingCredentialsException;
import com.netflix.spinnaker.credentials.definition.error.NoSuchCredentialsDefinitionException;
import com.netflix.spinnaker.kork.exceptions.InvalidCredentialsTypeException;
import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Provides exception handlers for {@link CredentialsDefinitionRepositoryException} exceptions. */
@RestControllerAdvice
public class CredentialsDefinitionAdvice {
  @ExceptionHandler(NoSuchCredentialsDefinitionException.class)
  public ResponseEntity<ModelMap> handleNoSuchCredentials(NoSuchCredentialsDefinitionException ex) {
    var map = convertExceptionToErrorAttributes(ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map);
  }

  @ExceptionHandler(DuplicateCredentialsDefinitionException.class)
  public ResponseEntity<Object> handleDuplicateCredentials(
      DuplicateCredentialsDefinitionException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .eTag(e.getETag())
        .lastModified(e.getLastModified())
        .body(e.getExisting());
  }

  @ExceptionHandler(NoMatchingCredentialsException.class)
  public ResponseEntity<Object> handleNoMatchingCredentials(NoMatchingCredentialsException e) {
    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
        .eTag(e.getETag())
        .lastModified(e.getLastModified())
        .body(e.getExisting());
  }

  @ExceptionHandler({
    InvalidCredentialsDefinitionException.class,
    InvalidCredentialsTypeException.class
  })
  public ResponseEntity<ModelMap> handleInvalidCredentials(SpinnakerException ex) {
    var map = convertExceptionToErrorAttributes(ex);
    return ResponseEntity.badRequest().body(map);
  }

  private static ModelMap convertExceptionToErrorAttributes(SpinnakerException ex) {
    ModelMap map = new ModelMap();
    map.addAttribute("timestamp", Instant.now());
    map.addAttribute("exception", ex.getClass().getName());
    map.addAttribute("message", ex.getMessage());
    map.addAllAttributes(ex.getAdditionalAttributes());
    return map;
  }
}
