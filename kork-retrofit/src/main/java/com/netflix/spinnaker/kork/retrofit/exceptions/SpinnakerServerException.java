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
import lombok.Getter;
import retrofit.RetrofitError;

/** Represents an error while attempting to execute a retrofit http client request. */
@NonnullByDefault
public class SpinnakerServerException extends SpinnakerException {
  @Getter @NonnullByDefault protected final String url;

  public SpinnakerServerException(String message, Throwable cause) {
    super(message, getProperCause(cause));
    this.url = getUrl(cause);
  }

  public SpinnakerServerException(Throwable cause) {
    super(cause.getMessage(), getProperCause(cause));
    this.url = getUrl(cause);
  }

  public SpinnakerServerException(RetrofitError re) {
    super(re.getMessage(), re.getCause());
    this.url = re.getUrl();
  }

  public SpinnakerServerException(retrofit2.Response<?> resp) {
    this.url = resp.raw().request().url().toString();
  }

  public SpinnakerServerException() {
    this.url = "";
  }

  /** Helper method to ensure that the url gets populated appropriately */
  private static String getUrl(Throwable cause) {
    if (cause instanceof RetrofitError) {
      return ((RetrofitError) cause).getUrl();
    }

    if (cause instanceof SpinnakerServerException) {
      return ((SpinnakerServerException) cause).getUrl();
    }

    return "";
  }

  private static Throwable getProperCause(Throwable cause) {
    if (cause instanceof RetrofitError) {
      return cause.getCause();
    }

    return cause;
  }

  @Override
  public SpinnakerServerException newInstance(String message) {
    return new SpinnakerServerException(message, this);
  }
}
