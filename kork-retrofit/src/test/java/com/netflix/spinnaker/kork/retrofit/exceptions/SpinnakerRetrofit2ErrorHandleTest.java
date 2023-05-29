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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
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
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SpinnakerRetrofit2ErrorHandleTest {

  private static Retrofit2Service retrofit2Service;

  private static final MockWebServer mockWebServer = new MockWebServer();

  private static String responseBodyString;

  private static okhttp3.OkHttpClient okHttpClient;

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
  public void testRetrofit404NotFoundIsNotRetryable() {
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
  public void testRetrofit400BadRequestIsNotRetryable() {
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
  public void testRetrofit410ClientErrorHasNullRetryable() {
    // We check the retryable strategy for 4xx errors. We expect that the retryable can be null.
    // Test a random 410 - Gone, a CLIENT_ERROR which does not have retryable.
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpStatus.GONE.value()).setBody(responseBodyString));
    SpinnakerHttpException spinnakerHttpException =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());
    assertNull(spinnakerHttpException.getRetryable());
  }

  @Test
  public void testRetrofitSimpleSpinnakerNetworkException() {
    mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
    SpinnakerNetworkException exception =
        assertThrows(
            SpinnakerNetworkException.class, () -> retrofit2Service.getRetrofit2().execute());
    String cause = exception.getCause().getMessage();
    assertNotNull(cause);
    assertNull(exception.getRetryable());
    assertTrue(cause.contains("timeout"));
  }

  @Test
  public void testRetrofitSimpleSpinnakerServerException() {
    mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
    SpinnakerServerException exception =
        assertThrows(
            SpinnakerServerException.class, () -> retrofit2Service.getRetrofit2().execute());
    String cause = exception.getCause().getMessage();
    assertNotNull(cause);
    assertNull(exception.getRetryable());
  }

  @Test
  public void testResponseHeadersIn404NotFoundException() {
    // Check response headers are retrievable from a SpinnakerHttpException
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value())
            .setBody(responseBodyString)
            .setHeader("Test", "true"));
    SpinnakerHttpException spinnakerHttpException =
        assertThrows(SpinnakerHttpException.class, () -> retrofit2Service.getRetrofit2().execute());
    assertTrue(spinnakerHttpException.getHeaders().containsKey("Test"));
    assertTrue(spinnakerHttpException.getHeaders().get("Test").contains("true"));
  }

  @Test
  public void testResponse401UnauthorizedCode() {
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

    // assert the headers
    assertTrue(exception.getHeaders().containsKey("Authorization"));
    assertTrue(exception.getHeaders().get("Authorization").contains("InvalidToken"));

    // Assert the expected error code (401)
    assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getResponseCode());
  }

  @Test
  public void testNotParameterizedException() {

    IllegalArgumentException illegalArgumentException =
        assertThrows(
            IllegalArgumentException.class,
            () -> retrofit2Service.testNotParameterized().execute());

    assertEquals(
        "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>",
        illegalArgumentException.getCause().getMessage());
  }

  @Test
  public void testWrongReturnTypeException() {

    IllegalArgumentException illegalArgumentException =
        assertThrows(
            IllegalArgumentException.class, () -> retrofit2Service.testWrongReturnType().execute());

    assertEquals(
        "Unable to create call adapter for interface com.netflix.spinnaker.kork.retrofit.exceptions.SpinnakerRetrofit2ErrorHandleTest$DummyWithExecute\n"
            + "    for method Retrofit2Service.testWrongReturnType",
        illegalArgumentException.getMessage());
  }

  @Test
  public void test200ResponseWithValidJson() throws Exception {
    // create a simple json string
    String responseBodyString = "{\"name\":\"test\"}";
    // mock successful response with a simple json string
    MockResponse mockResponse = new MockResponse().setBody(responseBodyString);
    mockWebServer.enqueue(mockResponse);

    // execute the custom retrofit service api which returns a map
    Response<Map<String, Object>> retrofit2Response =
        retrofit2Service.testCustomApiServiceForJsonResponse().execute();

    // verify successful response
    Assert.assertTrue(retrofit2Response.isSuccessful());
    assertNotNull(retrofit2Response.body());
    assertNull(retrofit2Response.errorBody());

    // validate the response
    Map<String, Object> retrofitResponseBody = retrofit2Response.body();
    assertTrue(retrofitResponseBody.get("name").equals("test"));
  }

  @Test
  public void test200ResponseWithInvalidJson() throws Exception {
    // create a simple invalid json string
    // "{\"name\":\"te";// incomplete json gives com.fasterxml.jackson.core.io.JsonEOFException
    String responseBodyString =
        "{\"name\"=\"test\"}"; // com.fasterxml.jackson.core.JsonParseException

    // mock successful response with this invalid json string
    MockResponse jsonMockResponse = new MockResponse().setBody(responseBodyString);
    mockWebServer.enqueue(jsonMockResponse);

    Call call = retrofit2Service.testCustomApiServiceForJsonResponse();
    // verify it throws exception while parsing the body.
    SpinnakerNetworkException spinnakerNetworkException =
        assertThrows(SpinnakerNetworkException.class, () -> call.execute());
    assertNull(spinnakerNetworkException.getRetryable());
    assertNull(spinnakerNetworkException.getResponseBody());
    assertNotNull(spinnakerNetworkException.getCause());
    assertTrue(
        spinnakerNetworkException
            .getCause()
            .toString()
            .contains("com.fasterxml.jackson.core.JsonParseException"));
  }

  @Test
  public void test200ResponseWithValidJsonList() throws Exception {
    // Actual Test - the quotes on attribute and value are required, else it will give
    // MalformedJsonException
    String responseBodyString =
        "{\"android\": [ {\"ver\":\"1.5\",\"name\":\"Cupcace\",\"api\":\"level3\",\"pic\":\"bane.jpg\"}]}";

    // In retrofit2 we can add multiple converters instead of overriding existing converters.
    //  Internally, Retrofit parses the response in specified order and proceeds to check next
    // converter if it fails.
    // Use GsonConverterFactory for json list with lenient option to allow invalid json strings also
    com.google.gson.Gson gson = new GsonBuilder().setLenient().create();
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addCallAdapterFactory(ErrorHandlingExecutorCallAdapterFactory.getInstance())
            .addConverterFactory(JacksonConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
    Retrofit2Service retrofit2Service2 = retrofit.create(Retrofit2Service.class);

    MockResponse jsonMockResponse = new MockResponse().setBody(responseBodyString);
    mockWebServer.enqueue(jsonMockResponse);
    Response<Map<String, Object>> jsonRetrofit2Response =
        retrofit2Service2.testCustomApiServiceForJsonResponse().execute();
    Assert.assertTrue(jsonRetrofit2Response.isSuccessful());
    assertNotNull(jsonRetrofit2Response.body());
    assertNull(jsonRetrofit2Response.errorBody());
  }

  @Test
  void test200ResponseWithValidByteArray() throws Exception {
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

  private Retrofit getMockRetrofit() {
    // mock the retrofit2.Retrofit class  without values
    Retrofit mockretrofit = mock(Retrofit.class);

    // mock the Retrofit builder behavior
    Retrofit.Builder b = mock(Retrofit.Builder.class);
    when(b.baseUrl(mockWebServer.url("/"))).thenReturn(b);
    when(b.client(okHttpClient)).thenReturn(b);
    when(b.addCallAdapterFactory(ErrorHandlingExecutorCallAdapterFactory.getInstance()))
        .thenReturn(b);
    when(b.build()).thenReturn(mockretrofit);
    return mockretrofit;
  }

  @Test
  void test200ResponseWithNullBody() throws IOException, JSONException {

    // We have to mock Retrofit, Retrofit2Service and Call<String> to test this scenario.
    // This is because when(mockObjectOnly).then(any()) is mandatory, means when() uses only a
    // mockObject.

    Retrofit mockretrofit = getMockRetrofit();
    Retrofit2Service mockRetrofit2Service = getMockRetrofit2Service(mockretrofit);
    Call<String> mockCall = getMockRetrofit2Call(mockRetrofit2Service);

    // Finally mock the behavior of the mockCall.execute() behavior.
    // Response.success(null) simulates a successful response with a null body
    when(mockCall.execute()).thenReturn(Response.success(null));

    // execute the retrofit2 API service call
    Call<String> call = mockRetrofit2Service.getRetrofit2();
    Response<String> retrofitResponse = call.execute();

    // check that both the response body and error body are null
    Assert.assertTrue(retrofitResponse.isSuccessful());
    assertNull(retrofitResponse.body());
    assertNull(retrofitResponse.errorBody());

    // Verify that the network call was executed
    verify(mockRetrofit2Service.getRetrofit2(), times(1)).execute();
  }

  private Call<String> getMockRetrofit2Call(Retrofit2Service mockRetrofit2Service) {
    // mock the Call<String> class without values
    Call<String> mockCall = mock(Call.class);
    // mock the  mockRetrofit2Service.getRetrofit2() network call behavior.
    when(mockRetrofit2Service.getRetrofit2()).thenReturn(mockCall);
    return mockCall;
  }

  private Retrofit2Service getMockRetrofit2Service(Retrofit mockretrofit) {
    // mock the Retrofit2Service class without values
    Retrofit2Service mockRetrofit2Service = mock(Retrofit2Service.class);
    // mock the method behavior mockRetrofit2Service.create() behavior
    when(mockretrofit.create(Retrofit2Service.class)).thenReturn(mockRetrofit2Service);

    return mockRetrofit2Service;
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
