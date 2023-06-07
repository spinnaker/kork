/*
 * Copyright 2023 OpsMx, Inc.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import okhttp3.*;
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

  private static final String CUSTOM_MESSAGE = "custom message";

  @BeforeAll
  public static void setupOnce() {
    retrofit2Service =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
  }

  /**
   * Test to handle a 200 - OK successful response. This scenario does not have any error, hence
   * does not require to handle a RetrofitException.
   */
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

  /**
   * Test to check that RetrofitException does not allow a null error body in the response. This is
   * due to the fail fast requireNonNull check added in the Retrofit constructor, instead of
   * checking for null in e.g. getBodyAs.
   */
  @Test
  public void testAnyRetrofitExceptionRequiresErrorBody() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), validJsonResponseBodyString);

    // We get a null error body for 200-OK successful response,
    // which we can use to check a RetrofitException instance creation.
    Response response = Response.success(HttpStatus.OK.value(), responseBody);
    assertNull(response.errorBody());

    // Gives the NullPointerException from assert in the private constructor
    assertThrows(
        NullPointerException.class, () -> RetrofitException.httpError(response, retrofit2Service));
  }

  /** Test to check that okhttp3 does not create ResponseBody if we try to transmit null content. */
  @Test
  public void testResponseBodyRequiresBodyContent() {
    final String contentJsonBodyString = null;
    // Test we cannot create a responseBody with null contents
    assertThrows(
        NullPointerException.class,
        () ->
            ResponseBody.create(
                MediaType.parse("application/json" + "; charset=utf-8"), contentJsonBodyString));
  }

  /**
   * Test to check that retrofit2 always requires a non-null ResponseBody to be present to create an
   * error response.
   */
  @Test
  public void testRetrofit2ResponseErrorRequiresResponseBody() {
    assertThrows(
        NullPointerException.class, () -> Response.error(HttpStatus.UNAUTHORIZED.value(), null));

    assertThrows(NullPointerException.class, () -> Response.error(null, null));
  }

  /**
   * Test to check that to create an instance of SpinnakerServerException by using a
   * RetrofitException, requires a complete and valid json in ResponseBody. RetrofitException uses
   * JacksonConverterFactory to convert the response body to HashMap and set it in the
   * SpinnakerServerException. If the json is incomplete or invalid, then the
   * JacksonConverterFactory throws a com.fasterxml.jackson.core.JsonParseException or any other
   * type of IOException while parsing. The IOException is converted to RuntimeException by
   * RetrofitException::getBodyAs()
   */
  @Test
  public void testSpinnakerHttpExceptionExpectsValidJsonErrorBody() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), invalidJsonResponseBodyString);
    Response response = Response.error(HttpStatus.NOT_FOUND.value(), responseBody);

    RetrofitException retrofitException = RetrofitException.httpError(response, retrofit2Service);
    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));
  }

  /**
   * Test to check that to create an instance of SpinnakerServerException by using a
   * RetrofitException, requires a complete and valid json in ResponseBody. RetrofitException uses
   * JacksonConverterFactory to convert the response body to HashMap and set it in the
   * SpinnakerServerException. If the json is incomplete or invalid, then the
   * JacksonConverterFactory throws a com.fasterxml.jackson.core.JsonParseException or any other
   * type of IOException while parsing. The IOException is converted to RuntimeException by
   * RetrofitException::getBodyAs(). To override the above behavior and to successfully parse a
   * byte-array or XML request/response body, requires to add a custom ByteConverterFactory or
   * XMLConverterFactory in the retrofit.
   */
  @Test
  public void testHttpNon2xxServerResponseWithByteArrayErrorBody() {
    byte[] byteArray = {0x12, 0x34, 0x56, 0x78};

    ResponseBody responseBody =
        ResponseBody.create(MediaType.parse("application/octet-stream"), byteArray);
    Response response = Response.error(HttpStatus.NOT_FOUND.value(), responseBody);
    RetrofitException retrofitException = RetrofitException.httpError(response, retrofit2Service);

    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));
  }

  /**
   * The Response error body is null to create SpinnakerServerException instance using
   * RetrofitException. This is because of below scenarios in the
   * ErrorHandlingExecutorCallAdapterFactory.ExecutorCallbackCall.execute() method. 1) If
   * delegate.execute() method returns isSuccessful() as TRUE, then a synchronous response is
   * present . 2) If delegate.execute() method returns SpinnakerHttpException, a synchronous
   * response is present. 3) If delegate.execute() method throws an
   * IOException(SpinnakerNetworkException), the synchronous response is null. Hence, error body is
   * null. 4) If delegate.execute() method throws a generic Exception(SpinnakerServerException), the
   * synchronous response is null. Hence, error body is null. The below test scenario is to check
   * case-4.
   */
  @Test
  public void testSpinnakerServerExceptionReturnsNullResponseBody() {
    Throwable cause = new Throwable("message");
    RetrofitException retrofitException = RetrofitException.unexpectedError(cause);
    assertNull(retrofitException.getBodyAs(HashMap.class));
  }

  /**
   * Test the custom headers are accessible in SpinnakerHttpException. Also test if custom message
   * is accessible in SpinnakerHttpException.
   */
  @Test
  public void testSpinnakerHttpExceptionHeaderAndMessage() {
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), validJsonResponseBodyString);
    Headers headers = new Headers.Builder().add("Retry-After", "60").build();
    okhttp3.Response rawErrorResponseWithHeaders =
        new okhttp3.Response.Builder()
            .code(HttpStatus.NOT_FOUND.value())
            .message(CUSTOM_MESSAGE)
            .protocol(Protocol.HTTP_1_1)
            .headers(headers)
            .request(new Request.Builder().url("http://localhost/").build())
            .build();
    Response errorResponse = Response.error(responseBody, rawErrorResponseWithHeaders);
    try {
      RetrofitException retrofitException =
          RetrofitException.httpError(errorResponse, retrofit2Service);
      throw new SpinnakerHttpException(retrofitException);

    } catch (SpinnakerHttpException spinnakerHttpException) {
      assertTrue(spinnakerHttpException.getHeaders().containsKey("Retry-After"));
      assertTrue(spinnakerHttpException.getHeaders().get("Retry-After").contains("60"));

      assertTrue(spinnakerHttpException.getMessage().contains(CUSTOM_MESSAGE));
      assertTrue(spinnakerHttpException.getRawMessage().contains(CUSTOM_MESSAGE));

      String expectedMessageFormat =
          String.format(
              "Status: %s, URL: http://localhost/, Message: %s %s",
              HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.value(), CUSTOM_MESSAGE);
      assertEquals(expectedMessageFormat, spinnakerHttpException.getMessage());
    }
  }
}
