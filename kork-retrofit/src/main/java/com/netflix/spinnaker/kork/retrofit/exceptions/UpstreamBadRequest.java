/*
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.spinnaker.kork.retrofit.exceptions;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static retrofit.RetrofitError.Kind.HTTP;

import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.util.Collection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * UpstreamBadRequest is used as a safe replacement for RetrofitError. RetrofitError poses issues
 * around serialization due to containing many unserializable fields. Retrofit v2 made changes that
 * allows for easy exception handling which converts the exception directly upon the response,
 * rather than what v1 does by wrapping the raw payload in a RetrofitError to later be handled by
 * users.
 *
 * @see <a href="https://github.com/square/retrofit/issues/669">GH issue #669</a>
 */
@Log4j2
public class UpstreamBadRequest extends SpinnakerException {
  @Getter private final int status;
  @Getter private final String url;
  @Getter private final Object error;

  public UpstreamBadRequest(RetrofitError cause) {
    // we do not pass the retrofit error into the super, since the error is not
    // serializable.
    super(cause.getMessage());
    this.setStackTrace(cause.getStackTrace());
    Response response = cause.getResponse();
    // response will be null for network retrofit errors
    status = (response != null) ? response.getStatus() : 0;
    url = cause.getUrl();
    Object body = null;
    try {
      // retrofit will pass a type when calling `getBody`, which may not be
      // serialiazable. So instead we will pass an object and let the object
      // mapper figure out what type is appropriate.
      body = cause.getBodyAs(Object.class);
    } catch (Exception e) {
      // exceptions that could potentially occur are around converting the
      // response to an object.
      //
      // A common reason could be that the response is not JSON and would thus
      // fail the conversion. So instead we will ignore the exception
      log.warn("failed to unmarshal body from response url={}", cause.getUrl(), e);
    }

    error = body;
  }

  @Deprecated(since = "2023-10-20", forRemoval = true)
  public static RuntimeException classifyError(RetrofitError error) {
    if (error.getKind() == HTTP
        && error.getResponse().getStatus() < INTERNAL_SERVER_ERROR.value()) {
      return new UpstreamBadRequest(error);
    } else {
      return error;
    }
  }

  @Deprecated(since = "2023-10-20", forRemoval = true)
  public static RuntimeException classifyError(
      RetrofitError error, Collection<Integer> supportedHttpStatuses) {
    if (error.getKind() == HTTP
        && supportedHttpStatuses.contains(error.getResponse().getStatus())) {
      return new UpstreamBadRequest(error);
    } else {
      return error;
    }
  }

  public static RuntimeException classifyError(SpinnakerServerException e) {
    Throwable cause = e.getCause();
    if (cause instanceof UpstreamBadRequest) {
      // this can only happen if the exception handler intercepted a
      // non-RetrofitError.
      return e;
    }

    RetrofitError re = e.getRetrofitError();
    if (re == null) {
      // this can occur if SpinnakerServerExceptions were constructed from a
      // throwable that was not a RetrofitError.
      return e;
    }

    // if we properly handled the RetrofitError, then this should not occur.
    return new UpstreamBadRequest(re);
  }
}
