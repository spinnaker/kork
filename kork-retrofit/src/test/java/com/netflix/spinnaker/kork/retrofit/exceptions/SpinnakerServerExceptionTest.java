/*
 * Copyright 2023 Salesforce, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SpinnakerServerExceptionTest {
  private static final String CUSTOM_MESSAGE = "custom message";

  @Test
  public void testSpinnakerHttpException_NewInstance() {
    Response response = new Response("http://localhost", 200, "reason", List.of(), null);
    try {
      RetrofitError error = RetrofitError.httpError("http://localhost", response, null, null);
      throw new SpinnakerHttpException(error);
    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerHttpException);
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
      assertEquals(response.getStatus(), ((SpinnakerHttpException) newException).getResponseCode());
    }
  }

  @Test
  public void testSpinnakerNetworkException_NewInstance() {
    IOException initialException = new IOException("message");
    try {
      RetrofitError error = RetrofitError.networkError("http://localhost", initialException);
      throw new SpinnakerNetworkException(error);
    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerNetworkException);
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
    }
  }

  @Test
  public void testSpinnakerServerException_NewInstance() {
    Throwable cause = new Throwable("message");
    try {
      RetrofitError error = RetrofitError.unexpectedError("http://localhost", cause);
      throw new SpinnakerServerException(error);
    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerServerException);
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
    }
  }

  @Test
  public void testSpinnakerServerException_responseBodyHttpException() {

    Retrofit retrofit2Service =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

    RetrofitException retrofitException =
        RetrofitException.httpError(
            retrofit2.Response.error(
                404,
                ResponseBody.create(
                    MediaType.parse("application/json" + "; charset=utf-8"),
                    "{\"name\":\"test\"}")),
            retrofit2Service);

    SpinnakerServerException serverException = new SpinnakerServerException(retrofitException);
    Map<String, Object> responseBody = serverException.getResponseBody();

    assertEquals(responseBody.size(), 1);
    assertEquals(responseBody.get("name"), "test");
  }

  @Test
  public void testSpinnakerServerException_responseBodyNetworkException() {

    RetrofitException retrofitException =
        RetrofitException.networkError(new IOException("Server not reachable"));
    SpinnakerServerException serverException = new SpinnakerServerException(retrofitException);

    Map<String, Object> responseBody = serverException.getResponseBody();
    assertEquals(responseBody, null);
  }

  @Test
  public void testSpinnakerServerException_responseBodyUnexpectedException() {

    RetrofitException retrofitException =
        RetrofitException.unexpectedError(new NullPointerException("npe"));
    SpinnakerServerException serverException = new SpinnakerServerException(retrofitException);

    Map<String, Object> responseBody = serverException.getResponseBody();
    assertEquals(responseBody, null);
  }
}
