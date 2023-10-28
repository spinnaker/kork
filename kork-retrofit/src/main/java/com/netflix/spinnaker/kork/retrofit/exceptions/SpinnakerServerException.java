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
import retrofit2.Response;

/** Represents an error while attempting to execute a retrofit http client request. */
@NonnullByDefault
public class SpinnakerServerException extends SpinnakerException {

  private final String url;

  public SpinnakerServerException(RetrofitError e) {
    super(e.getMessage(), e.getCause());
    url = e.getUrl();
  }

  public SpinnakerServerException(Response retrofit2Response) {
    super();
    url = retrofit2Response.raw().request().url().toString();
  }

  public SpinnakerServerException(Throwable cause, String url) {
    super(cause);
    this.url = url;
  }

  public SpinnakerServerException(String message, Throwable cause, String url) {
    super(message, cause);
    this.url = url;
  }

  public SpinnakerServerException(String message, SpinnakerServerException cause) {
    super(message, cause);
    this.url = cause.getUrl();
  }

  @Override
  public SpinnakerServerException newInstance(String message) {
    return new SpinnakerServerException(message, this);
  }

  public String getUrl() {
    return this.url;
  }
}
