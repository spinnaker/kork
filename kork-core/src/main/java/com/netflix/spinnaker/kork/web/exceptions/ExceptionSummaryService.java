package com.netflix.spinnaker.kork.web.exceptions;

import com.netflix.spinnaker.kork.api.exceptions.ExceptionSummary;
import com.netflix.spinnaker.kork.api.exceptions.ExceptionSummary.TraceDetail;
import com.netflix.spinnaker.kork.api.exceptions.ExceptionSummary.TraceDetail.TraceDetailBuilder;
import com.netflix.spinnaker.kork.exceptions.HasAdditionalAttributes;
import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a {@link ExceptionSummary} object from a given Exception. This object is meant to help
 * provide context to end-users to help resolve issues, while still offering enough detail for
 * operators and developers to trace internal bugs.
 */
public class ExceptionSummaryService {

  private final UserMessageService userMessageService;

  public ExceptionSummaryService(UserMessageService userMessageService) {
    this.userMessageService = userMessageService;
  }

  /**
   * Provides the {@link ExceptionSummary} given the thrown exception.
   *
   * @param throwable {@link Throwable}
   * @return {@link ExceptionSummary}
   */
  public ExceptionSummary summary(Throwable throwable) {
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
            .filter(d -> d.getRetryable() != null)
            .findFirst()
            .map(TraceDetail::getRetryable)
            .orElse(null);

    return ExceptionSummary.builder()
        .cause(details.get(details.size() - 1).getMessage())
        .message(details.get(0).getMessage())
        .details(reversedDetails)
        .retryable(retryable)
        .build();
  }

  private TraceDetail createTraceDetail(Throwable throwable) {
    TraceDetailBuilder detailBuilder = TraceDetail.builder().message(throwable.getMessage());

    if (throwable instanceof SpinnakerException) {
      SpinnakerException spinnakerException = (SpinnakerException) throwable;

      detailBuilder
          .userMessage(
              userMessageService.userMessage(throwable, spinnakerException.getUserMessage()))
          .retryable(spinnakerException.getRetryable());
    }
    if (throwable instanceof HasAdditionalAttributes) {
      detailBuilder.additionalAttributes(
          ((HasAdditionalAttributes) throwable).getAdditionalAttributes());
    }

    return detailBuilder.build();
  }
}
