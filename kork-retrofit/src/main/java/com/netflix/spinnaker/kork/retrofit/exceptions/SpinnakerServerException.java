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
import java.io.Serializable;
import lombok.Getter;
import retrofit.RetrofitError;

/** Represents an error while attempting to execute a retrofit http client request. */
@NonnullByDefault
public class SpinnakerServerException extends SpinnakerException {
  @Getter private transient RetrofitError retrofitError = null;

  public SpinnakerServerException(String message, Throwable cause) {
    super(message, makeSerializable(cause));

    if (cause instanceof RetrofitError) {
      this.retrofitError = (RetrofitError) cause;
    }
  }

  public SpinnakerServerException(Throwable cause) {
    super(makeSerializable(cause));

    if (cause instanceof RetrofitError) {
      this.retrofitError = (RetrofitError) cause;
    }
  }

  public SpinnakerServerException() {}

  @Override
  public SpinnakerServerException newInstance(String message) {
    return new SpinnakerServerException(message, this);
  }

  /**
   * RetrofitErrors are not serializable which causes issues with anything that utilizes the {@link
   * Serializable}, e.g. session storage in Spring.
   */
  private static Throwable makeSerializable(Throwable cause) {
    if (!(cause instanceof RetrofitError)) {
      return cause;
    }

    // here we convert the retrofit error to something serializable.
    return new UpstreamBadRequest((RetrofitError) cause);
  }
}
