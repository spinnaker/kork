/*
 * Copyright 2021 Salesforce, Inc.
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

import com.netflix.spinnaker.kork.exceptions.HasAdditionalAttributes;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Log4j2
public class BaseExceptionHandlers extends ResponseEntityExceptionHandler {
  private final DefaultErrorAttributes defaultErrorAttributes = new DefaultErrorAttributes();

  protected final ExceptionMessageDecorator exceptionMessageDecorator;

  public BaseExceptionHandlers(ExceptionMessageDecorator exceptionMessageDecorator) {
    this.exceptionMessageDecorator = exceptionMessageDecorator;
  }

  protected void storeException(
      HttpServletRequest request, HttpServletResponse response, Exception ex) {
    // store exception as an attribute of HttpServletRequest such that it can be referenced by
    // GenericErrorController
    defaultErrorAttributes.resolveException(request, response, null, ex);
  }

  /**
   * Generic exception handler for Spring exceptions. This transforms the exception details in a
   * similar fashion to {@link DefaultErrorAttributes} combined with {@link
   * com.netflix.spinnaker.kork.web.controllers.GenericErrorController}. Many standard Spring
   * exceptions have default handlers in {@link ResponseEntityExceptionHandler} which mostly choose
   * an appropriate HTTP response status while providing no other useful information outside of
   * debug logs.
   *
   * @param ex the exception
   * @param body the body for the response
   * @param headers the headers for the response
   * @param status the response status
   * @param request the current request
   * @return the error response
   */
  @Override
  @Nonnull
  @ParametersAreNonnullByDefault
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    log.info("Handling generic exception with response status {}", status, ex);
    ModelMap map = new ModelMap();
    map.addAttribute("timestamp", Instant.now());
    map.addAttribute("exception", ex.getClass().getName());
    map.addAttribute("error", status.getReasonPhrase());
    map.addAttribute("status", status.value());
    if (ex instanceof HasAdditionalAttributes) {
      map.addAllAttributes(((HasAdditionalAttributes) ex).getAdditionalAttributes());
    }
    if (ex instanceof Errors) {
      // like BindException
      Errors errors = (Errors) ex;
      String message =
          "Validation failed for object '"
              + errors.getObjectName()
              + "' with error count "
              + errors.getErrorCount();
      map.addAttribute("message", message);
      map.addAttribute("errors", errors.getAllErrors());
    } else {
      String message = ex.getMessage();
      if (StringUtils.hasText(message)) {
        map.addAttribute("message", message);
      }
    }
    return ResponseEntity.status(status).headers(headers).body(map);
  }
}
