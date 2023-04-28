/*
 * Copyright 2020 Google, Inc.
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

package com.netflix.spinnaker.kork.retrofit.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import retrofit.RetrofitError;

/** An exception that exposes the message of a {@link RetrofitError}, or a custom message. */
@NonnullByDefault
public class SpinnakerServerException extends SpinnakerException {

  /**
   * A message derived from a RetrofitError's response body, or null if a custom message has been
   * provided.
   */
  private final String rawMessage;

  /* RetrofitError's response body derived as map or null if response not provided in RetrofitError or custom  message has been provided*/
  private final Map<String, Object> responseBody;

  /**
   * Parses the message from the {@link RetrofitErrorResponseBody} and parses response of a {@link
   * RetrofitError} responseBody.
   *
   * @param e The {@link RetrofitError} thrown by an invocation of the {@link retrofit.RestAdapter}
   */
  public SpinnakerServerException(RetrofitError e) {
    super(e.getCause());
    RetrofitErrorResponseBody body =
        (RetrofitErrorResponseBody) e.getBodyAs(RetrofitErrorResponseBody.class);
    this.rawMessage =
        Optional.ofNullable(body).map(RetrofitErrorResponseBody::getMessage).orElse(e.getMessage());
    this.responseBody = (Map<String, Object>) e.getBodyAs(HashMap.class);
  }

  /**
   * Parses the Error response body of {@link RetrofitException} as responseBody.
   *
   * @param e The {@link RetrofitException} thrown by an invocation of the {@link
   *     retrofit2.Retrofit}
   */
  public SpinnakerServerException(RetrofitException e) {
    super(e.getCause());
    this.responseBody = e.getErrorBodyAs(HashMap.class);
    this.rawMessage =
        responseBody != null
            ? (String) responseBody.getOrDefault("message", e.getMessage())
            : e.getMessage();
  }

  /**
   * Construct a SpinnakerServerException with a specified message, instead of deriving one from a
   * response body.
   *
   * @param message the message
   * @param cause the cause. Note that this is required (i.e. can't be null) since in the absence of
   *     a cause or a RetrofitError that provides the cause, SpinnakerServerException is likely not
   *     the appropriate exception class to use.
   */
  public SpinnakerServerException(String message, Throwable cause) {
    super(message, cause);
    rawMessage = null;
    responseBody = null;
  }

  public Map<String, Object> getResponseBody() {
    return responseBody;
  }

  @Override
  public String getMessage() {
    if (rawMessage == null) {
      return super.getMessage();
    }
    return rawMessage;
  }

  final String getRawMessage() {
    return rawMessage;
  }

  @Getter
  // Use JsonIgnoreProperties because some responses contain properties that
  // cannot be mapped to the RetrofitErrorResponseBody class.  If the default
  // JacksonConverter (with no extra configurations) is used to deserialize the
  // response body and properties other than "message" exist in the JSON
  // response, there will be an UnrecognizedPropertyException.
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class RetrofitErrorResponseBody {
    private final String message;

    @JsonCreator
    RetrofitErrorResponseBody(@JsonProperty("message") String message) {
      this.message = message;
    }
  }

  @Override
  public SpinnakerServerException newInstance(String message) {
    return new SpinnakerServerException(message, this);
  }
}
