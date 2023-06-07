/*
 * Copyright 2023 Netflix, Inc.
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitExceptionTest {

  private static Retrofit retrofit2Service;

  private final String validJsonResponseBodyString = "{\"name\":\"test\"}";

  // An invalid json response body by replacing the colon(:) with an equals(=) symbol.
  private final String invalidJsonResponseBodyString = "{\"name\"=\"test\"}";

  @BeforeAll
  public static void setupOnce() {
    retrofit2Service =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
  }

  @Test
  public void testHttp200ServerResponseReturnsBodyAndNullErrorBody() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), validJsonResponseBodyString);
    Response response = Response.success(responseBody);

    assertEquals(HttpStatus.OK.value(), response.code());
    assertNull(response.errorBody());

    assertNotNull(response.body());
  }

  @Test
  public void testAnyRetrofitExceptionRequiresErrorBody() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), validJsonResponseBodyString);
    Response response = Response.success(HttpStatus.OK.value(), responseBody);
    assertNull(response.errorBody());

    // Gives the NullPointerException from assert in the private constructor
    // because for 200-OK successful response the error body is null.
    assertThrows(
        NullPointerException.class, () -> RetrofitException.httpError(response, retrofit2Service));
  }

  @Test
  public void testAnyHttpNon2xxServerResponseReturnsResponseErrorBodyAndErrorMessage() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), validJsonResponseBodyString);
    Response response = Response.error(HttpStatus.NOT_FOUND.value(), responseBody);

    RetrofitException retrofitException = RetrofitException.httpError(response, retrofit2Service);
    assertNotNull(retrofitException.getResponse());
    //This is the default error message set in retrofit2.Response.error(int code, ResponseBody body) method.
    //if the message is not set in the raw response.
    assertEquals(retrofitException.getResponse().raw().message(), "Response.error()");

    Map<String, String> responseBodyMap = retrofitException.getBodyAs(HashMap.class);
    assertNotNull(responseBodyMap);
    assertEquals(responseBodyMap.get("name"), "test");

    assertNotNull(retrofitException.getMessage());
    assertTrue(
        retrofitException.getMessage().contains(String.valueOf(HttpStatus.NOT_FOUND.value())));
  }

  @Test
  public void testAnyHttpNon2xxServerResponseExpectsValidJsonErrorBody() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), invalidJsonResponseBodyString);
    Response response = Response.error(HttpStatus.NOT_FOUND.value(), responseBody);

    // com.fasterxml.jackson.core.JsonParseException is a kind of IOException
    // Hence it is converted to RuntimeException from RetrofitException::getBodyAs()
    RetrofitException retrofitException = RetrofitException.httpError(response, retrofit2Service);
    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));

    // invalid json from server response can be accessed from raw message.
    assertNotNull(retrofitException.getResponse());
     //This is the default error message set in retrofit2.Response.error(int code, ResponseBody body) method.
    //if the message is not set in the raw response.
    assertEquals(retrofitException.getResponse().raw().message(), "Response.error()");
  }

  @Test
  public void testIOExceptionServerResponseReturnsOnlyErrorMessageAndNullResponse() {
    IOException ioException = new IOException("custom IOException"); // server returned IOException

    RetrofitException retrofitException = RetrofitException.networkError(ioException);
    assertNull(retrofitException.getResponse());

    assertNotNull(retrofitException.getMessage());
    assertEquals("custom IOException", retrofitException.getMessage());

    Map<String, String> responseBodyMap = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBodyMap);
  }

  @Test
  public void testAnyExceptionServerResponseReturnsOnlyErrorMessageAndNullResponse() {
    Throwable cause =
        new Throwable("custom Throwable"); // For example if server returned NullPointerException

    RetrofitException retrofitException = RetrofitException.unexpectedError(cause);
    Map<String, String> responseBodyMap = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBodyMap);

    assertNotNull(retrofitException.getMessage());
    assertEquals("custom Throwable", retrofitException.getMessage());

    assertNull(retrofitException.getResponse());
  }
}
