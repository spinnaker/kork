/*
 * Copyright 2018 Netflix, Inc.
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
package com.netflix.spinnaker.kork.telemetry;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented {
  /**
   * If set, this value will be used as an override to the metric name that would normally be
   * generated automatically. This can be useful for when there may be metric name collisions.
   *
   * @return Optional metric name override
   */
  String metricName() default "";

  /**
   * @return If set to true, the {@code InstrumentedProxy} will not instrument the associated
   *     method.
   */
  boolean ignore() default false;

  /** @return Sequence of alternating key/value tag pairs. Expects key as first element. */
  String[] tags() default {};
}
