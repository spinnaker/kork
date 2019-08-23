/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.netflix.spinnaker.kork.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Wither;

/**
 * Builds a summary object from a given Exception. This object is meant to help provide context to
 * end-users resolve their own issues, while still offering enough detail for operators and
 * developers to trace internal bugs.
 */
@Getter
@Builder
@Wither
public class ExceptionSummary {

  /** The primary factory method for an ExceptionSummary. */
  public static ExceptionSummary from(Throwable throwable) {

    List<TraceDetail> details = new ArrayList<>();

    Throwable cause = throwable;
    do {
      details.add(createTraceDetail(cause));
      cause = cause.getCause();
    } while (cause != null);

    List<TraceDetail> reversedDetails = new ArrayList<>(details);
    Collections.reverse(reversedDetails);
    Boolean retryable =
        reversedDetails.stream()
            .filter(d -> d.retryable != null)
            .findFirst()
            .map(d -> d.retryable)
            .orElse(null);

    return ExceptionSummary.builder()
        .cause(details.get(details.size() - 1).message)
        .message(details.get(0).message)
        .details(reversedDetails)
        .retryable(retryable)
        .build();
  }

  private static TraceDetail createTraceDetail(Throwable throwable) {
    TraceDetail.TraceDetailBuilder detailBuilder =
        TraceDetail.builder().message(throwable.getMessage());

    if (throwable instanceof SpinnakerException) {
      SpinnakerException spinnakerException = (SpinnakerException) throwable;
      detailBuilder
          .userMessage(spinnakerException.getUserMessage())
          .retryable(spinnakerException.getRetryable());
    }
    if (throwable instanceof HasAdditionalAttributes) {
      detailBuilder.additionalAttributes(
          ((HasAdditionalAttributes) throwable).getAdditionalAttributes());
    }

    return detailBuilder.build();
  }

  /**
   * The upper-most exception message in a cause chain. Typically speaking, this message is fairly
   * generic.
   */
  private String message;

  /**
   * The bottom-most exception message in a cause change. This message should have the exact cause
   * of what went wrong.
   */
  private String cause;

  /**
   * The message and cause properties should be enough, but if that's not the case, or additional
   * context is required, the details list is automatically populated using the additional metadata
   * provided by {@code SpinnakerException} derivative classes. This list is ordered in reverse,
   * where the first element is the deepest exception in a cause chain.
   */
  private List<TraceDetail> details;

  /**
   * Whether or not the particular operation should be considered retryable. This is informed by any
   * {@code SpinnakerException} in the cause chain, where exceptions that are higher in the cause
   * chain can overwrite the flag set by exceptions below it. If no {@code SpinnakerException} is
   * present, the value is left null and the retry behavior is undefined and can be decided by the
   * client.
   */
  private Boolean retryable;

  @Getter
  @Builder
  @Wither
  public static class TraceDetail {
    /** The exception message. */
    private String message;

    /** The user-specific message, as defined by {@code SpinnakerException} derivative classes. */
    private String userMessage;

    /** Additional attributes, as defined by {@code HasAdditionalAttributes}. */
    private Map<String, Object> additionalAttributes;

    /** The retryable flag for the particular Exception. */
    private Boolean retryable;
  }
}
