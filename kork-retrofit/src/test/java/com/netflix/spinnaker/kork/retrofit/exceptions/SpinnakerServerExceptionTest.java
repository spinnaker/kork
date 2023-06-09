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

import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.io.IOException;
import java.util.List;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.apache.groovy.json.internal.CharBuf;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SpinnakerServerExceptionTest {
  private static final String CUSTOM_MESSAGE = "custom message";
  private final String validJsonResponseBodyString = "{\"name\":\"test\"}";

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
  public void testSpinnakerHttpExceptionNewInstanceFromRetrofitException() {
    retrofit2.Response response = retrofit2.Response.error(
      HttpStatus.NOT_FOUND.value(),
      ResponseBody.create(
        MediaType.parse("application/json" + "; charset=utf-8"),
        validJsonResponseBodyString));
    Retrofit retrofit2Service =
      new Retrofit.Builder()
        .baseUrl("http://localhost:8987/")
        .addConverterFactory(JacksonConverterFactory.create())
        .build();

    try {
      RetrofitException retrofitException = RetrofitException.httpError( response, retrofit2Service);
      throw new SpinnakerHttpException(retrofitException);
    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerHttpException);
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
      assertEquals(HttpStatus.NOT_FOUND.value(), ((SpinnakerHttpException) newException).getResponseCode());
    }
  }


  @Test
  public void testSpinnakerNetworkExceptionNewInstanceFromRetrofitException() {
    IOException initialException = new IOException("Server not reachable");
    try {
      RetrofitException retrofitException = RetrofitException.networkError(initialException);
      throw new SpinnakerNetworkException(retrofitException);
    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerNetworkException);
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
      assertNull(((SpinnakerNetworkException) newException).getRawMessage());
    }
  }


  @Test
  public void testSpinnakerServerExceptionNewInstanceFromRetrofitException() {
    Throwable cause = new Throwable("message");
    try {
      RetrofitException retrofitException = RetrofitException.unexpectedError(cause);
      throw new SpinnakerServerException(retrofitException);

    } catch (SpinnakerException e) {
      SpinnakerException newException = e.newInstance(CUSTOM_MESSAGE);

      assertTrue(newException instanceof SpinnakerServerException);
      assertEquals(CUSTOM_MESSAGE, newException.getMessage());
      assertEquals(e, newException.getCause());
      assertNull(((SpinnakerServerException) newException).getRawMessage());
    }
  }

}
