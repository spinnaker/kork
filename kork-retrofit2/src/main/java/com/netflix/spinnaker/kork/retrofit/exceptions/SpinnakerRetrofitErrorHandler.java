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

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import org.springframework.http.HttpStatus;

/**
 * An error handler to be registered with a {@link retrofit2.Retrofit}. Allows clients to catch a
 * SpinnakerServerException or something more specific (e.g. {@link SpinnakerHttpException}, or
 * {@link SpinnakerNetworkException}) depending on the properties of the {@link RetrofitException})
 */
@NonnullByDefault
public final class SpinnakerRetrofitErrorHandler {
  private SpinnakerRetrofitErrorHandler() {}

  /**
   * Returns an instance of a {@link SpinnakerRetrofitErrorHandler}.
   *
   * @return An instance of {@link SpinnakerRetrofitErrorHandler}
   */
  public static SpinnakerRetrofitErrorHandler getInstance() {
    return new SpinnakerRetrofitErrorHandler();
  }

  /**
   * Returns a more specific {@link Throwable} depending on properties of the caught {@link
   * RetrofitException}.
   *
   * @param e The {@link RetrofitException} thrown by an invocation of the {@link
   *     retrofit2.Retrofit}
   * @return A more informative {@link Throwable}
   */
  public Throwable handleError(RetrofitException e) {
    switch (e.getKind()) {
      case HTTP:
        SpinnakerHttpException retval = new SpinnakerHttpException(e);
        if ((e.getResponse().code() == HttpStatus.NOT_FOUND.value())
            || (e.getResponse().code() == HttpStatus.BAD_REQUEST.value())) {
          retval.setRetryable(false);
        }
        return retval;
      case NETWORK:
        return new SpinnakerNetworkException(e);
      default:
        return new SpinnakerServerException(e);
    }
  }
}
