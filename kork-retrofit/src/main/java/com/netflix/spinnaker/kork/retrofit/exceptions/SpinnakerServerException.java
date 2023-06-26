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
import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import retrofit.RetrofitError;

/** An exception that exposes the message of a {@link RetrofitError}, or a custom message. */
@NonnullByDefault
public class SpinnakerServerException extends SpinnakerException {

  /**
   * Parses the message from the {@link RetrofitError}.
   *
   * @param e The {@link RetrofitError} thrown by an invocation of the {@link retrofit.RestAdapter}
   */
  public SpinnakerServerException(RetrofitError e) {
    super(e.getMessage(), e.getCause());
  }

  public SpinnakerServerException(RetrofitException e) {
    super(e.getMessage(), e.getCause());
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
  }

  @Override
  public SpinnakerServerException newInstance(String message) {
    return new SpinnakerServerException(message, this);
  }
}
