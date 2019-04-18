package com.netflix.spinnaker.okhttp;

import com.netflix.spectator.api.BasicTag;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

/**
 * Sort of generic class to wrap okhttp and okhttp3 interception logic since we basically do the
 * same thing and it's annoying to duplicate it
 */
class MetricsInterceptor {
  private final Registry registry;
  private final Logger log;

  MetricsInterceptor(Registry registry) {
    this.registry = registry;
    this.log = LoggerFactory.getLogger(getClass());
  }

  Object intercept(Object chainObject) throws IOException {
    long start = System.nanoTime();
    boolean wasSuccessful = false;
    int statusCode = -1;

    Interceptor.Chain chain =
        (chainObject instanceof Interceptor.Chain) ? (Interceptor.Chain) chainObject : null;
    okhttp3.Interceptor.Chain chain3 =
        (chainObject instanceof okhttp3.Interceptor.Chain)
            ? (okhttp3.Interceptor.Chain) chainObject
            : null;

    Request request = (chain != null) ? chain.request() : null;
    okhttp3.Request request3 = (chain3 != null) ? chain3.request() : null;

    List<String> missingHeaders = new ArrayList<>();
    String method = null;
    URL url = null;

    try {
      String xSpinAnonymous = MDC.get(AuthenticatedRequest.Header.XSpinnakerAnonymous);

      if (xSpinAnonymous == null) {
        for (AuthenticatedRequest.Header header : AuthenticatedRequest.Header.values()) {
          String headerValue =
              (request != null)
                  ? request.header(header.getHeader())
                  : request3.header(header.getHeader());

          if (header.isRequired() && StringUtils.isEmpty(headerValue)) {
            missingHeaders.add(header.getHeader());
          }
        }
      }

      Object response = null;

      if (chain != null) {
        method = request.method() + ":" + request.url();
        url = request.url();
        response = chain.proceed(request);
        statusCode = ((Response) response).code();
      } else {
        method = request3.method() + ":" + request3.url();
        url = request3.url().url();
        response = chain3.proceed(request3);
        statusCode = ((okhttp3.Response) response).code();
      }

      wasSuccessful = true;
      return response;
    } finally {
      boolean missingAuthHeaders = missingHeaders.size() > 0;

      if (missingAuthHeaders) {
        List<String> stack =
            Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::toString)
                .filter(x -> x.contains("com.netflix.spinnaker"))
                .collect(Collectors.toList());

        String stackTrace = String.join("\n\tat ", stack);
        log.warn(
            String.format(
                "Request %s:%s does not have authentication headers and will be treated as anonymous.\nat %s",
                method, url, stackTrace));
      }

      recordTimer(
          registry, url, System.nanoTime() - start, statusCode, wasSuccessful, !missingAuthHeaders);
      recordMissingHeaders(registry, missingHeaders);
    }
  }

  private static void recordMissingHeaders(Registry registry, List<String> missingHeaders) {
    registry
        .counter(
            "okhttp.requests.missingHeaders",
            missingHeaders.stream()
                .map(header -> new BasicTag("header", header))
                .collect(Collectors.toList()))
        .increment();
  }

  private static void recordTimer(
      Registry registry,
      URL requestUrl,
      Long durationNs,
      int statusCode,
      boolean wasSuccessful,
      boolean hasAuthHeaders) {
    registry
        .timer(
            registry
                .createId("okhttp.requests")
                .withTag("requestHost", requestUrl.getHost())
                .withTag("statusCode", String.valueOf(statusCode))
                .withTag("status", bucket(statusCode))
                .withTag("success", wasSuccessful)
                .withTag("authenticated", hasAuthHeaders))
        .record(durationNs, TimeUnit.NANOSECONDS);
  }

  private static String bucket(int statusCode) {
    if (statusCode < 0) {
      return "Unknown";
    }

    return Integer.toString(statusCode).charAt(0) + "xx";
  }
}
