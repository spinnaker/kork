/*
 * Copyright 2023 OpsMx, Inc.
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
import java.util.Objects;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * The {@link RetrofitException} class is similar to {@link retrofit.RetrofitError} as RetrofitError
 * class is removed in retrofit2. To handle the exception globally and achieve similar logic as
 * retrofit in retrofit2, this exception used along with {@link
 * com.netflix.spinnaker.kork.retrofit.ErrorHandlingExecutorCallAdapterFactory}.
 */
public class RetrofitException extends RuntimeException {
  public static RetrofitException httpError(Response response, Retrofit retrofit) {
    String message = response.code() + " " + response.message();
    return new RetrofitException(message, response, retrofit);
  }

  /** Response from server, which contains causes for the failure */
  private final Response response;

  /**
   * Client used while the service creation, which has convertor logic to be used to parse the
   * response
   */
  private final Retrofit retrofit;

  RetrofitException(String message, Response response, Retrofit retrofit) {
    super(message);

    this.response = response;
    if (response != null) {
      // Fail fast instead of checking for null in e.g. getErrorBodyAs.
      Objects.requireNonNull(response.errorBody());
    }
    this.retrofit = retrofit;
  }

  /** Response object containing status code, headers, body, etc. */
  public Response getResponse() {
    return response;
  }

  /**
   * HTTP response body converted to specified {@code type}. {@code null} if there is no response.
   *
   * @throws RuntimeException wrapping the underlying IOException if unable to convert the body to
   *     the specified {@code type}.
   */
  public <T> T getErrorBodyAs(Class<T> type) {
    if (response == null) {
      return null;
    }

    Converter<ResponseBody, T> converter = retrofit.responseBodyConverter(type, new Annotation[0]);
    try {
      return converter.convert(response.errorBody());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
