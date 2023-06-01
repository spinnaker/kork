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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitExceptionTest {

  private static Retrofit retrofit2Service;

  private static String responseBodyString;

  private final String validJsonResponseBodyString = "{\"name\":\"test\"}";

  // An invalid json response body by replacing the colon(:) with an equals(=) symbol.
  private final String invalidJsonResponseBodyString = "{\"name\"=\"test\"}";

  // Create a responseBody with invalid or incomplete Json or jsonlist contents.
  String invalidContentJsonBodyString =
      "{\"android\": [ {\"ver\":\"1.5\",\"name\":\"Cupcace\",\"api\":\"level3\",\"pic\":\"ba";

  @BeforeAll
  public static void setupOnce() throws Exception {

    Map<String, String> responseBodyMap = new HashMap<>();
    responseBodyMap.put("timestamp", "123123123123");
    responseBodyMap.put("message", "Something happened error message");
    responseBodyString = new ObjectMapper().writeValueAsString(responseBodyMap);

    retrofit2Service =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
  }

  @Test
  public void testHttpSuccessResponseReturns200ServerBodyNullErrorBody() {
    // Test for 200 to 299 responses.
    Response<String> response = Response.success(validJsonResponseBodyString);

    // Assert that the response is not successful
    assertTrue(response.isSuccessful());

    // Assert the response code
    assertEquals(200, response.code());

    // Assert the response body is correct.
    String responseBody = response.body();
    assertNotNull(responseBody);

    // returns the body as-is returned by server.
    assertEquals(validJsonResponseBodyString, responseBody);

    // Assert the response body
    ResponseBody errorBody = response.errorBody();
    assertNull(errorBody);
  }

  @Test
  public void testNetworkWithIOExceptionReturnsNullErrorBodyWithServerError() {
    // Assume the server returned IOException
    IOException ioException = new IOException("custom IOException");

    // Use networkError RetrofitException for IOException.
    RetrofitException retrofitException = RetrofitException.networkError(ioException);

    Map<String, String> responseBodyMap = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBodyMap);

    // Retrofit2Exception contains  error message which response returned.
    assertEquals("custom IOException", retrofitException.getMessage());

    // Since the server has thrown an IOException, hence the cause can be retrieved.
    assertNotNull(retrofitException.getCause());

    // Check if the request encountered an IOException
    Throwable serverException = retrofitException.getCause();
    assertTrue(serverException instanceof IOException);
  }

  @Test
  public void testUnexpectedWithNullExceptionReturnsNullErrorBodyWithServerError() {
    // Assume the server returned NullPointerException
    NullPointerException nullPointerException =
        new NullPointerException("custom NullPointerException");

    // Use unexpectedError RetrofitException for non-IOExceptions like NullPointerException.
    RetrofitException retrofitException = RetrofitException.unexpectedError(nullPointerException);

    Map<String, String> responseBodyMap = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBodyMap);

    // Retrofit2Exception contains  error message which response returned.
    assertEquals("custom NullPointerException", retrofitException.getMessage());

    // Since the server has thrown an NullPointerException, hence the cause can be retrieved.
    assertNotNull(retrofitException.getCause());

    // Check if the request encountered an NullPointerException
    Throwable serverException = retrofitException.getCause();
    assertTrue(serverException instanceof NullPointerException);
  }

  @Test
  public void test4xxInvalidJsonBodyConversionExceptionWithMessageAndNoCause() {
    RetrofitException retrofitException =
        RetrofitException.httpError(
            Response.error(
                HttpStatus.NOT_FOUND.value(),
                ResponseBody.create(
                    MediaType.parse("application/json" + "; charset=utf-8"),
                    invalidJsonResponseBodyString)),
            retrofit2Service);

    // Gives JsonParseException, or MalformedJsonException,
    // or JsonEOFException or IllegalStateException, or
    // some other exception for json parsing, which is a type of RuntimeException.
    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));

    // Retrofit2Exception contains 404 error message which response returned.
    assertEquals("404 Response.error()", retrofitException.getMessage());

    // Since server did not return any Exception like IOException or NullPointerException,
    // hence the cause is null.
    assertNull(retrofitException.getCause());
  }

  @Test
  public void testNetwork500InvalidJsonToNullBodyMessageAndCause() {
    // Assume the server returned IOException
    IOException networkException = new IOException("custom IOException");

    // Create network RetrofitException
    RetrofitException retrofitException = RetrofitException.networkError(networkException);

    // The response body is null same as network RetrofitError .
    assertNull(retrofitException.getBodyAs(HashMap.class));

    // Retrofit2Exception contains the IOException error message which the response had returned.
    assertEquals("custom IOException", retrofitException.getMessage());

    // Since the server has thrown an IOException, hence the cause can be retrieved.
    assertNotNull(retrofitException.getCause());
  }

  @Test
  public void test4xxValidJsonToHashMapWithMessageAndNoCause() {
    RetrofitException retrofitException =
        RetrofitException.httpError(
            Response.error(
                HttpStatus.NOT_FOUND.value(),
                ResponseBody.create(
                    MediaType.parse("application/json" + "; charset=utf-8"),
                    validJsonResponseBodyString)),
            retrofit2Service);

    // successfully allows to parse the response body.
    Map<String, String> responseBody = retrofitException.getBodyAs(HashMap.class);
    assertEquals("test", responseBody.get("name"));

    // Retrofit2Exception contains 404 error message which response returned.
    assertEquals("404 Response.error()", retrofitException.getMessage());

    // Since server did not return any Exception like IOException or NullPointerException,
    // hence the cause is null.
    assertNull(retrofitException.getCause());
  }

  @Test
  public void testHttp4xxNullBodyContentThrowsException() throws Exception {
    final String contentJsonBodyString = null;

    // Test we cannot create a responseBody with null contents
    assertThrows(
        NullPointerException.class,
        () ->
            ResponseBody.create(
                MediaType.parse("application/json" + "; charset=utf-8"), contentJsonBodyString));
  }

  @Test
  public void testHttp4xxNullBodyThrowsException() throws Exception {

    // We cannot create a response with null ResponseBody
    assertThrows(
        NullPointerException.class, () -> Response.error(HttpStatus.UNAUTHORIZED.value(), null));
  }

  @Test
  public void testHttp4xxInvalidJsonlistBodyContentThrowsException() throws Exception {

    RetrofitException retrofitException =
        RetrofitException.httpError(
            Response.error(
                HttpStatus.NOT_FOUND.value(),
                ResponseBody.create(
                    MediaType.parse("application/json" + "; charset=utf-8"),
                    invalidContentJsonBodyString)),
            retrofit2Service);

    // Gives the json parsing exception depending on the json and the ConverterFactory added to
    // retrofit2
    // However, all the exceptions are of the type RuntimeException.
    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));
  }

  @Test
  public void testHttp4xxParseByteArrayBodyContentUsingConverter() throws Exception {
    // Create a responseBody with byte array contents.
    // It allows the API service call to execute but gives MalformedJsonException
    // error when reading the body from the retrofitException into the HashMap.
    byte[] byteArray = {0x12, 0x34, 0x56, 0x78};

    ResponseBody responseBody =
        ResponseBody.create(MediaType.parse("application/octet-stream"), byteArray);
    Response<?> actualResponse = Response.error(HttpStatus.UNAUTHORIZED.value(), responseBody);
    assertNull(actualResponse.body());
    ResponseBody errorBody = actualResponse.errorBody();
    assertNotNull(errorBody);

    // add the ByteArrayConverterFactoryTestUtil to convert response body to bytes.
    Retrofit customRetrofit =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(ByteArrayConverterFactoryTestUtil.create())
            .build();

    // get the retrofit2 http exception from the actual response
    RetrofitException retrofitException =
        RetrofitException.httpError(actualResponse, customRetrofit);
    assertNull(retrofitException.getCause());
    assertEquals(retrofitException.getMessage(), "401 Response.error()");

    // hashmap for json body
    // Without the ByteArrayConverterFactoryTestUtil, and with only GsonConverterFactory,
    // the getBodyAs() will give a com.fasterxml.jackson.core.JsonParseException
    byte[] actualResponseBodyContent = retrofitException.getBodyAs(byte[].class);
    assertNotNull(actualResponseBodyContent);
    assertEquals(actualResponseBodyContent.length, byteArray.length);

    for (int i = 0; i < byteArray.length; i++) {
      assertEquals(byteArray[i], actualResponseBodyContent[i]);
    }
  }

  interface Retrofit2Service {
    @retrofit2.http.GET("/retrofit2")
    Call<String> foo();
  }
}
