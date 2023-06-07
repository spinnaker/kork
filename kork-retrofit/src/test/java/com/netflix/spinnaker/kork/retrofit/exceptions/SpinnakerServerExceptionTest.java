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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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

  /**
   * Test the HTTP details like response status code, error message, error body json, and other
   * details from SpinnakerHttpException for a non-2xx server response.
   */
  @Test
  public void testSpinnakerServerException_responseBodyHttpException() {
    final String validJsonResponseBodyString = "{\"name\":\"test\"}";

    Retrofit retrofit2Service =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

    try {
      RetrofitException retrofitException =
          RetrofitException.httpError(
              retrofit2.Response.error(
                  HttpStatus.NOT_FOUND.value(),
                  ResponseBody.create(
                      MediaType.parse("application/json" + "; charset=utf-8"),
                      validJsonResponseBodyString)),
              retrofit2Service);
      throw new SpinnakerHttpException(retrofitException);
    } catch (SpinnakerHttpException spinnakerHttpException) {
      /* Map<String, Object> responseBody = serverException.getResponseBody();
      assertEquals(responseBody.size(), 1);
      assertEquals(responseBody.get("name"), "test");*/
      assertTrue(
          spinnakerHttpException
              .getMessage()
              .contains(
                  String.valueOf(
                      HttpStatus.NOT_FOUND
                          .value()))); // Default error message if not set, is of format "404
      // Response.error()"
      assertTrue(
          spinnakerHttpException
              .getRawMessage()
              .contains(
                  String.valueOf(
                      HttpStatus.NOT_FOUND
                          .value()))); // Default raw message if not set, is of format "404
      // Response.error()"
      assertNull(spinnakerHttpException.getCause()); // SpinnakerHttpException does not have a cause
      assertEquals(
          HttpStatus.NOT_FOUND.value(),
          spinnakerHttpException.getResponseCode()); // Response code is HTTP status code
    }
  }

  /**
   * Test the Exception details like cause, error message if server throws a network IOException or
   * SpinnakerNetworkException
   */
  @Test
  public void testSpinnakerServerException_responseBodyNetworkException() {
    IOException initialException = new IOException("Server not reachable");
    try {
      RetrofitException retrofitException = RetrofitException.networkError(initialException);
      throw new SpinnakerNetworkException(retrofitException);
    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerNetworkException);
      /*Map<String, Object> responseBody = serverException.getResponseBody();
      assertEquals(responseBody, null);*/
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
      assertNull(((SpinnakerNetworkException) newException).getRawMessage());
    }
  }

  /**
   * Test the Exception details like cause, error message if server throws any application Exception
   * or any generic SpinnakerServerException
   */
  @Test
  public void testSpinnakerServerException_responseBodyUnexpectedException() {
    Throwable cause = new Throwable("message");
    try {
      RetrofitException retrofitException = RetrofitException.unexpectedError(cause);
      throw new SpinnakerServerException(retrofitException);

    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerServerException);
      /*Map<String, Object> responseBody = serverException.getResponseBody();
      assertEquals(responseBody, null);*/
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
      assertNull(((SpinnakerServerException) newException).getRawMessage());
    }
  }
}
