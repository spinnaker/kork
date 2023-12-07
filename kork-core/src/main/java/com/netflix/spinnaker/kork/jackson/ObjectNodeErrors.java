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

package com.netflix.spinnaker.kork.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import org.springframework.validation.AbstractBindingResult;

/**
 * Generic {@link org.springframework.validation.Errors} container for validating a parsed JSON
 * object.
 */
@NonnullByDefault
public class ObjectNodeErrors extends AbstractBindingResult {
  private final ObjectNode target;

  public ObjectNodeErrors(ObjectNode target, String objectName) {
    super(objectName);
    this.target = target;
  }

  @Override
  public Object getTarget() {
    return target;
  }

  @Override
  protected Object getActualFieldValue(String field) {
    return target.get(field);
  }
}
