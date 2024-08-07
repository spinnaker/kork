/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.moniker;

import com.netflix.spinnaker.orchestration.OperationDescription;

/**
 * A "Namer" takes some type "T" (e.g. a server group) and allows you to either
 *
 * <p>a) derive a "Moniker" object from a "T" object b) set a "Moniker" object on a "T" object
 *
 * <p>For example, if T is an ASG, and you apply the Moniker(app=app, sequence=3), the ASG Namer
 * would set ASG.name = app-v003
 *
 * @param <T> is the type of the object acting as the name
 */
public interface Namer<T> {
  void applyMoniker(T obj, Moniker moniker);

  /**
   * Sets a moniker on the given object with additional context provided by the description. This
   * default implementation ignores the provided {@code description} and simply delegates to the
   * {@link #applyMoniker(Object, Moniker)} method.
   *
   * @param obj the object to which the moniker will be applied
   * @param moniker the moniker to apply
   * @param description a description that provides additional context for the operation
   */
  default void applyMoniker(T obj, Moniker moniker, OperationDescription description) {
    applyMoniker(obj, moniker);
  }

  Moniker deriveMoniker(T obj);
}
