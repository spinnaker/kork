/*
 * Copyright 2022 Netflix, Inc.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RetrofitException extends RuntimeException {
  public static RetrofitException httpError(String url, Response response, Retrofit retrofit) {
    String message = response.code() + " " + response.message();
    return new RetrofitException(message, url, response, Kind.HTTP, null, retrofit);
  }

  public static RetrofitException networkError(IOException exception) {
    return new RetrofitException(exception.getMessage(), null, null, Kind.NETWORK, exception, null);
  }

  public static RetrofitException unexpectedError(Throwable exception) {
    return new RetrofitException(
        exception.getMessage(), null, null, Kind.UNEXPECTED, exception, null);
  }

  /** Identifies the event kind which triggered a {@link RetrofitException}. */
  public enum Kind {
    /** An {@link IOException} occurred while communicating to the server. */
    NETWORK,
    /** A non-200 HTTP status code was received from the server. */
    HTTP,
    /**
     * An internal error occurred while attempting to execute a request. It is best practice to
     * re-throw this exception so your application crashes.
     */
    UNEXPECTED
  }

  private final String url;
  private final Response response;
  private final Kind kind;
  private final Retrofit retrofit;

  RetrofitException(
      String message,
      String url,
      Response response,
      Kind kind,
      Throwable exception,
      Retrofit retrofit) {
    super(message, exception);
    this.url = url;
    this.response = response;
    this.kind = kind;
    this.retrofit = retrofit;
  }

  /** The request URL which produced the error. */
  public String getUrl() {
    return url;
  }

  /** Response object containing status code, headers, body, etc. */
  public Response getResponse() {
    return response;
  }

  /** The event kind which triggered this error. */
  public Kind getKind() {
    return kind;
  }

  /** The Retrofit this request was executed on */
  public Retrofit getRetrofit() {
    return retrofit;
  }

  /**
   * HTTP response body converted to specified {@code type}. {@code null} if there is no response.
   *
   * @throws IOException if unable to convert the body to the specified {@code type}.
   */
  public <T> T getBodyAs(Class<T> type) {
    if (response == null || response.errorBody() == null) {
      return null;
    }
    try {
      Converter<ResponseBody, T> converter =
          retrofit.responseBodyConverter(type, new Annotation[0]);
      return converter.convert(response.errorBody());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
