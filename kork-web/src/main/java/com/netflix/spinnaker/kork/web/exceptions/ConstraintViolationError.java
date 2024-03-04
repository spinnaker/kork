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

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** Summarizes a {@link javax.validation.ConstraintViolation}. */
@Getter
@ToString
@Builder
public class ConstraintViolationError {
  private final String message;
  private final String errorCode;
  private final String path;
  @Builder.Default private final Map<String, Object> additionalAttributes = Map.of();
}
