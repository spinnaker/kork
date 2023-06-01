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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.retrofit.ErrorHandlingExecutorCallAdapterFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SpinnakerRetrofit2ErrorHandleTest {

  private static Retrofit2Service retrofit2Service;

  private static final MockWebServer mockWebServer = new MockWebServer();

  private static String responseBodyString;

  private static okhttp3.OkHttpClient okHttpClient;

  private final String validJsonResponseBodyString = "{\"name\":\"test\"}";

  // An invalid json response body by replacing the colon(:) with an equals(=) symbol.
  private final String invalidJsonResponseBodyString = "{\"name\"=\"test\"}";

  private final String jsonlistBodyContentString =
      "{\"android\": [ {\"ver\":\"1.5\",\"name\":\"Cupcace\",\"api\":\"level3\",\"pic\":\"bane.jpg\"}]}";

  @BeforeAll
  public static void setupOnce() throws Exception {

    Map<String, String> responseBodyMap = new HashMap<>();
    responseBodyMap.put("timestamp", "123123123123");
    responseBodyMap.put("message", "Something happened error message");
    responseBodyString = new ObjectMapper().writeValueAsString(responseBodyMap);

    okHttpClient =
        new OkHttpClient.Builder()
            .callTimeout(1, TimeUnit.SECONDS)
            .connectTimeout(1, TimeUnit.SECONDS)
            .build();

    retrofit2Service =
        new Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(okHttpClient)
            .addCallAdapterFactory(ErrorHandlingExecutorCallAdapterFactory.getInstance())
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(Retrofit2Service.class);
  }

  @AfterAll
  public static void shutdownOnce() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  public void testHttp404NotFoundIsNotRetryable() {
    // We check the retryable strategy for 4xx errors.
    // Test the 404 status code is not retryable.
    // Let us assume that the server responded with 404-NOT_FOUND.
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value())
            .setBody(responseBodyString));
    SpinnakerHttpException notFoundException =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());
    assertNotNull(notFoundException.getRetryable());
    assertFalse(notFoundException.getRetryable());
  }

  @Test
  public void testHttp400BadRequestIsNotRetryable() {
    // We check the retryable strategy for 4xx errors.
    // Test the 400-BadRequest is not retryable.
    // Let us assume that the server responded with  400-BAD_REQUEST.
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.BAD_REQUEST.value())
            .setBody(responseBodyString));
    SpinnakerHttpException spinnakerHttpException =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());
    assertNotNull(spinnakerHttpException.getRetryable());
    assertFalse(spinnakerHttpException.getRetryable());
  }

  @Test
  public void testHttp4xxIsNullRetryable() {
    // We check the retryable strategy for 4xx errors. We expect that the retryable can be null.
    // Test a random 410 - Gone, a CLIENT_ERROR which does not have retryable.
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpStatus.GONE.value()).setBody(responseBodyString));
    SpinnakerHttpException spinnakerHttpException =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());
    assertNull(spinnakerHttpException.getRetryable());
  }

  @Test
  public void testNetwork500IsNullRetryable() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .setSocketPolicy(SocketPolicy.NO_RESPONSE));
    SpinnakerNetworkException exception =
        assertThrows(
            SpinnakerNetworkException.class, () -> retrofit2Service.getRetrofit2().execute());

    assertNull(exception.getRetryable());
  }

  @Test
  public void testSpinnakerExceptionIsNullRetryable() {
    mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
    SpinnakerServerException exception =
        assertThrows(
            SpinnakerServerException.class, () -> retrofit2Service.getRetrofit2().execute());

    assertNull(exception.getRetryable());
  }

  @Test
  public void testHttp404HasHeadersAndResponsebodyAndMessage() {
    // create a simple json string
    final String CONTENT_TYPE = "Content-Type";
    final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    // create a mock response with the headers and a valid json body
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value())
            .setBody(validJsonResponseBodyString)
            .addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE));

    // API service call
    SpinnakerHttpException spinnakerHttpException =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());

    // Check response body is retrievable from a SpinnakerHttpException via its parent
    // SpinnakerServerException.
    Map<String, Object> retrofitResponseBody = spinnakerHttpException.getResponseBody();
    assertNotNull(spinnakerHttpException.getResponseBody());
    assertTrue(retrofitResponseBody.get("name").equals("test"));

    // SpinnakerHttpException creates the message in this format: "Status: %s, URL: %s, Message:
    // %s".
    String errorMessage = spinnakerHttpException.getMessage();
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains("Status: " + HttpStatus.NOT_FOUND.value()));

    // Check response headers are retrievable from a SpinnakerHttpException
    assertTrue(spinnakerHttpException.getHeaders().containsKey(CONTENT_TYPE));
    assertTrue(spinnakerHttpException.getHeaders().get(CONTENT_TYPE).contains(JSON_CONTENT_TYPE));
  }

  @Test
  public void testNetwork500HasMessageServerErrorAndNullResponseBody() {

    // Enqueue a response that causes an IOException
    mockWebServer.enqueue(
        new MockResponse()
            .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
            .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .setBody(validJsonResponseBodyString));

    // API service call
    SpinnakerNetworkException spinnakerNetworkException =
        assertThrows(
            SpinnakerNetworkException.class, () -> retrofit2Service.getRetrofit2().execute());

    // Check response body is null for
    // IOException/SpinnakerNetworkException/SpinnakerServerException similar to RetrofitError
    assertNull(spinnakerNetworkException.getResponseBody());

    // SpinnakerNetworkException returns the error message as-is sent by the server.
    assertNotNull(spinnakerNetworkException.getMessage());
    assertTrue(spinnakerNetworkException.getMessage().contains("unexpected end of stream"));

    // Since the server has thrown an IOException, hence the cause can be retrieved.
    assertNotNull(spinnakerNetworkException.getCause());

    // Check if the request encountered an IOException
    Throwable serverException = spinnakerNetworkException.getCause();
    assertTrue(serverException instanceof IOException);
  }

  @Test
  public void testUnexpected500HasMessageServerErrorAndNullResponseBody() throws IOException {
    // Enqueue a response that causes an non-IOException

    mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
    SpinnakerServerException spinnakerServerException =
        assertThrows(
            SpinnakerServerException.class, () -> retrofit2Service.getRetrofit2().execute());

    // Check response body is null for
    // IOException/SpinnakerNetworkException/SpinnakerServerException similar to RetrofitError
    assertNull(spinnakerServerException.getResponseBody());

    // Check response body is Null
    // SpinnakerNetworkException returns the error message as-is sent by the server.
    assertNotNull(spinnakerServerException.getMessage());
    assertTrue(
        spinnakerServerException.getMessage().contains("No content to map due to end-of-input"));

    // Since the server has thrown an NullPointerException, hence the cause can be retrieved.
    assertNotNull(spinnakerServerException.getCause());

    // Check if the request encountered an NullPointerException
    Throwable serverException = spinnakerServerException.getCause();

    assertTrue(
        serverException instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException);
  }

  @Test
  public void testHttp401ResponseIsNotRetryableByDefault() {
    // Check response headers are retrievable from 401-UNAUTHORIZED SpinnakerHttpException
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.UNAUTHORIZED.value())
            .setBody(responseBodyString)
            .setHeader("Authorization", "InvalidToken"));

    // make actual API call
    SpinnakerHttpException exception =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());

    // 401-UNAUTHORIZED is not retryable
    assertNull(exception.getRetryable());
  }

  @Test
  public void testApiServiceCallResponseIsAlwaysParameterized() {
    IllegalArgumentException illegalArgumentException =
        assertThrows(
            IllegalArgumentException.class,
            () -> retrofit2Service.testNotParameterized().execute());

    assertEquals(
        "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>",
        illegalArgumentException.getCause().getMessage());
  }

  @Test
  public void testRetrofit2GetApiServiceIsAlwaysOfTypeCall() {

    IllegalArgumentException illegalArgumentException =
        assertThrows(
            IllegalArgumentException.class, () -> retrofit2Service.testWrongReturnType().execute());

    assertEquals(
        "Unable to create call adapter for interface com.netflix.spinnaker.kork.retrofit.exceptions.SpinnakerRetrofit2ErrorHandleTest$DummyWithExecute\n"
            + "    for method Retrofit2Service.testWrongReturnType",
        illegalArgumentException.getMessage());
  }

  @Test
  public void testHttp2xxValidJsonConvertsToHashMap() throws IOException {
    Call call = retrofit2Service.testCustomApiServiceForJsonResponse();

    // mock successful response with a simple json string
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setBody(validJsonResponseBodyString));

    // actual API service call does not throw any Exception or error.
    // There is no conversion exception.
    Response<Map<String, Object>> jsonRetrofit2Response = call.execute();

    // verify successful response
    Assert.assertTrue(jsonRetrofit2Response.isSuccessful());

    // validate response body data
    assertNotNull(jsonRetrofit2Response.body());

    Map<String, Object> responseBody = jsonRetrofit2Response.body();
    assertEquals(responseBody.get("name"), "test");

    // verify that the server response errorBody is null
    assertNull(jsonRetrofit2Response.errorBody());
  }

  @Test
  public void testHttp2xxInvalidJsonThrowsConversionException() throws Exception {
    // mock successful response with invalid json string
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setBody(invalidJsonResponseBodyString));

    Call call = retrofit2Service.getRetrofit2();

    // verify it throws exception while parsing the body and converting to a hashmap.
    SpinnakerNetworkException spinnakerNetworkException =
        assertThrows(SpinnakerNetworkException.class, () -> call.execute());

    // Check response body is retrievable from a SpinnakerHttpException via its parent
    // SpinnakerServerException.
    Map<String, Object> retrofitResponseBody = spinnakerNetworkException.getResponseBody();
    assertNull(spinnakerNetworkException.getResponseBody());
    // assertTrue(retrofitResponseBody.get("name").equals("test"));

    // Check error message is retrievable from a SpinnakerHttpException via its parent
    // SpinnakerServerException.
    // SpinnakerHttpException creates the message in this format: "Status: %s, URL: %s, Message:
    // %s".
    String errorMessage = spinnakerNetworkException.getMessage();
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains("Cannot deserialize value of type"));
  }

  @Test
  public void testHttp2xxValidJsonListConvertsToHashMap() throws Exception {
    Call call = retrofit2Service.testCustomApiServiceForJsonResponse();

    // mock successful response with a simple json string
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setBody(jsonlistBodyContentString));

    // actual API service call does not throw any Exception or error.
    // There is no conversion exception.
    Response<Map<String, Object>> jsonRetrofit2Response = call.execute();

    // assert the response is successful
    Assert.assertTrue(jsonRetrofit2Response.isSuccessful());

    // check the response has a body and validate the data.
    assertNotNull(jsonRetrofit2Response.body());
    Map<String, Object> responseBody = jsonRetrofit2Response.body();
    assertNotNull(responseBody.get("android"));

    // assert the response has no error body
    assertNull(jsonRetrofit2Response.errorBody());
  }

  @Test
  void testHttp2xxConvertsToByteArrayByUsingCustomConverter() throws Exception {
    // To test byte[] response we have to create the custom converter to convert the ResponseBody to
    // a byte[]:
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addCallAdapterFactory(ErrorHandlingExecutorCallAdapterFactory.getInstance())
            .addConverterFactory(ByteArrayConverterFactoryTestUtil.create())
            .build();
    Retrofit2Service retrofit2Service2 = retrofit.create(Retrofit2Service.class);

    // create a mock response with byte string
    byte[] byteArray = {0x12, 0x34, 0x56, 0x78};
    String byteBodyString = new String(byteArray, StandardCharsets.UTF_8);
    MockResponse byteMockResponse = new MockResponse().setBody(byteBodyString);
    mockWebServer.enqueue(byteMockResponse);

    // execute the retrofit2 API service call
    Response<byte[]> byteRetrofit2Response =
        retrofit2Service2.testCustomApiServiceForByteResponse().execute();

    // check the retrofit2 response
    Assert.assertTrue(byteRetrofit2Response.isSuccessful());
    assertNotNull(byteRetrofit2Response.body());
    assertNull(byteRetrofit2Response.errorBody());

    // validate the data from retrofit2 response
    byte[] responseBodyByteArray = byteRetrofit2Response.body();
    assertEquals(byteArray.length, responseBodyByteArray.length);
    for (int i = 0; i < byteArray.length; i++) {
      assertEquals(byteArray[i], responseBodyByteArray[i]);
    }
  }

  interface Retrofit2Service {
    @retrofit2.http.GET("/retrofit2")
    Call<String> getRetrofit2();

    @retrofit2.http.GET("/retrofit2/para")
    Call testNotParameterized();

    @retrofit2.http.GET("/retrofit2/wrongReturnType")
    DummyWithExecute testWrongReturnType();

    // Add the API service method to test for a successful 200-OK byte array retrofit2 response
    @retrofit2.http.GET("/retrofit2/testByteArray")
    Call<byte[]> testCustomApiServiceForByteResponse();

    // Add the API service method to test for a successful 200-OK json/gson/jsonlist retrofit2
    // response
    @retrofit2.http.GET("/retrofit2/jsonresponse")
    Call<Map<String, Object>> testCustomApiServiceForJsonResponse();
  }

  interface DummyWithExecute {
    void execute();
  }
}
