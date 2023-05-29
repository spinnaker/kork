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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitExceptionTest {

  private static Retrofit retrofit2Service;

  private static String responseBodyString;

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
  public void testHttpException() {
    RetrofitException retrofitException =
        RetrofitException.httpError(
            Response.error(
                404,
                ResponseBody.create(
                    MediaType.parse("application/json" + "; charset=utf-8"), responseBodyString)),
            retrofit2Service);

    Map<String, String> responseBody = retrofitException.getBodyAs(HashMap.class);
    assertEquals("Something happened error message", responseBody.get("message"));
  }

  @Test
  public void testHttpConversionFail() {
    RetrofitException retrofitException =
        RetrofitException.httpError(
            Response.error(
                404,
                ResponseBody.create(
                    MediaType.parse("application/json" + "; charset=utf-8"),
                    "{\"name\"=\"test\"}")),
            retrofit2Service);

    // Gives MalformedJsonException or JsonEOFException or IllegalStateException, or
    // some other exception for json parsing, which is a type of RuntimeException.
    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));
  }

  @Test
  public void testNetworkError() {
    RetrofitException retrofitException =
        RetrofitException.networkError(new IOException("io exception occured"));
    Map<String, String> responseBody = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBody);
  }

  @Test
  public void testServerException() {
    MediaType.get("application/json; charset=utf-8");
    RetrofitException retrofitException =
        RetrofitException.unexpectedError(new NullPointerException("np"));
    Map<String, String> responseBody = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBody);
  }

  @Test
  public void test2xxSuccessResponseWithType() {
    // Test for 200 to 299 responses.
    Response<String> response = Response.success(responseBodyString);
    String body = response.body();
    assertNotNull(body);

    // Assert the response body
    ResponseBody errorBody = response.errorBody();
    assertNull(errorBody);
    assertThrows(
        NullPointerException.class, () -> RetrofitException.httpError(response, retrofit2Service));
  }

  @Test
  public void test2xxSuccessResponseWithWildcard() {
    // Test for 200 to 299 responses.

    Retrofit retrofit2 =
        new Retrofit.Builder()
            .baseUrl("http://localhost:8987/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

    Response<?> response = Response.success(responseBodyString);
    assertNotNull(response);
    String body = (String) response.body();
    assertNotNull(body);

    // Assert the response body
    ResponseBody errorBody = response.errorBody();
    assertNull(errorBody);
    assertThrows(
        NullPointerException.class, () -> RetrofitException.httpError(response, retrofit2));
  }

  @Test
  public void test401UnauthorizedHttpError() throws Exception {
    // create simple json body contents
    Map<String, String> contentBodyMap = new HashMap<>();
    contentBodyMap.put("timestamp", "123123123123");
    contentBodyMap.put("message", "Something happened error message");
    String contentJsonBodyString = new ObjectMapper().writeValueAsString(contentBodyMap);

    this.testApiServiceForJsonResponse(contentBodyMap, contentJsonBodyString);
  }

  @Test
  public void testNullBodyContentFor4xxHttpError() throws Exception {
    final String contentJsonBodyString = null;

    // Test we cannot create a responseBody with null contents
    assertThrows(
        NullPointerException.class,
        () ->
            ResponseBody.create(
                MediaType.parse("application/json" + "; charset=utf-8"), contentJsonBodyString));
  }

  @Test
  public void testNullResponseBodyFor4xxHttpError() throws Exception {

    // We cannot create a response with null ResponseBody
    assertThrows(
        NullPointerException.class, () -> Response.error(HttpStatus.UNAUTHORIZED.value(), null));
  }

  @Test
  public void testInvalidJsonResponseBodyFor4xxHttpError() throws Exception {
    // Create a responseBody with invalid or incomplete Json contents.
    // It allows the API service call to execute but gives MalformedJsonException
    // error when reading the body from the retrofitException into the HashMap.
    String invalidContentJsonBodyString = "{\"name\":\"te";
    Map<String, String> expectedContentBodyMap = new HashMap<>();
    expectedContentBodyMap.put("name", "test");

    // verify it throws MalformedJsonException
    try {
      this.testApiServiceForJsonResponse(expectedContentBodyMap, invalidContentJsonBodyString);
    } catch (Exception exception) {
      assertTrue(exception.getMessage().contains("com.google.gson.stream.MalformedJsonException"));
    }
  }

  @Test
  public void testInvalidJsonListResponseBodyFor4xxHttpError() throws Exception {
    // Create a responseBody with invalid or incomplete Json or jsonlist contents.
    // It allows the API service call to execute but gives error when reading the body from the
    // retrofitException into the HashMap.
    // But it gives com.fasterxml.jackson.core.io.JsonEOFException if we use a
    // JacksonConverterFactory.
    // GsonConverterFactory will give a more detailed com.google.gson.stream.MalformedJsonException.
    String invalidContentJsonBodyString =
        "{\"android\": [ {\"ver\":\"1.5\",\"name\":\"Cupcace\",\"api\":\"level3\",\"pic\":\"ba";

    // Use the valid string to create a map from the json list, otherwise it will give
    // MalformedJsonException here itself.
    // Added lenient option to allow invalid json strings also
    com.google.gson.Gson gson = new GsonBuilder().setLenient().create();
    Map contentBodyMap = gson.fromJson(responseBodyString, Map.class);
    assertNotNull(contentBodyMap);

    try {
      this.testApiServiceForJsonResponse(contentBodyMap, invalidContentJsonBodyString);
    } catch (RuntimeException exception) {
      // Gives java.lang.MalformedJsonException or some other exception for json parsing, which is a
      // type of RuntimeException
      assertTrue(exception.getMessage().contains("com.google.gson.stream.MalformedJsonException"));
    }
  }

  @Test
  public void testByteArrayWithConverterFor4xxHttpError() throws Exception {
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

  private void testApiServiceForJsonResponse(Map expectedContentBodyMap, String jsonContentString)
      throws IOException {
    // default converter is gson when factory list is null
    this.testApiServiceForJsonResponse(expectedContentBodyMap, jsonContentString, null);
  }

  private void testApiServiceForJsonResponse(
      Map expectedContentBodyMap,
      String jsonContentString,
      List<Converter.Factory> converterFactoryList)
      throws IOException {
    // Here we have randomly picked 401 - UNAUTHORIZED error.
    // However, our main intent is to check the RetrofitException behaviour for different response
    // bodies.

    // validate contents cannot be null, otherwise it gives NullPointerException
    assertNotNull(jsonContentString);

    String invalidContentJsonBodyString = (String) jsonContentString;
    // validate contents cannot be null, otherwise it gives NullPointerException
    ResponseBody responseBody =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), invalidContentJsonBodyString);

    // validate responseBody cannot be null, otherwise it gives NullPointerException
    assertNotNull(responseBody);

    Response<?> actualResponse = Response.error(HttpStatus.UNAUTHORIZED.value(), responseBody);
    assertNotNull(actualResponse);

    // assert response body is null
    String actualBody = (String) actualResponse.body();
    assertNull(actualBody);

    // Assert the error body is there
    ResponseBody errorBody = actualResponse.errorBody();
    assertNotNull(errorBody);

    // create a builder
    Retrofit.Builder customRetrofitBuilder =
        new Retrofit.Builder().baseUrl("http://localhost:8987/");

    // Use default as GsonConverterFactory if not specified,  for json list with lenient option to
    // allow invalid json strings also
    if (converterFactoryList == null || converterFactoryList.size() == 0) {
      com.google.gson.Gson gson = new GsonBuilder().setLenient().create();
      converterFactoryList = new ArrayList<>();
      converterFactoryList.add(GsonConverterFactory.create(gson));
    }
    // Here, order is important. Retrofit2 first parses with first converter.
    // If it fails with first converter, then tries to parse with next.
    for (Converter.Factory factory : converterFactoryList) {
      customRetrofitBuilder.addConverterFactory(factory);
    }

    // build the retrofit2.
    Retrofit customRetrofit = customRetrofitBuilder.build();

    // get the retrofit2 http exception from the actual response
    RetrofitException retrofitException =
        RetrofitException.httpError(actualResponse, customRetrofit);
    assertNull(retrofitException.getCause());
    assertEquals(retrofitException.getMessage(), "401 Response.error()");

    // validate the body contents in the exception
    try {
      // hashmap for json body
      Map actualResponseBodyContentMap = retrofitException.getBodyAs(HashMap.class);

      assertNotNull(actualResponseBodyContentMap);
      assertEquals(actualResponseBodyContentMap.size(), expectedContentBodyMap.size());

      // validate data by comparing eg
      // assertEquals(actualResponseBodyContentMap.get("name"), expectedContentBodyMap.get("test"));
      for (Object key : expectedContentBodyMap.keySet()) {
        assertEquals(expectedContentBodyMap.get(key), actualResponseBodyContentMap.get(key));
      }
    } catch (RuntimeException exception) {
      // Gives java.lang.IllegalStateException or some other exception for json parsing, which is a
      // type of RuntimeException
      throw exception;
    }
  }

  @Test
  public void testNetworkException() throws IOException {
    // create an IOException.
    IOException networkException = new IOException("Network Exception");

    // Assume server returned some abrupt  INTERNAL_SERVER_ERROR exception when it got above
    // IOException.
    // The body details are not nullable
    ResponseBody body =
        ResponseBody.create(
            MediaType.parse("application/json" + "; charset=utf-8"), "{\"name\":\"test\"}");
    Response<?> actualResponse = Response.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), body);
    assertNotNull(actualResponse);
    networkException.initCause(new retrofit2.HttpException(actualResponse));

    // Now we try to parse the retrofit2 network exception from the actual response
    RetrofitException retrofitException = RetrofitException.networkError(networkException);
    assertNotNull(retrofitException.getCause());
    String cause = retrofitException.getCause().getMessage();
    String localizedMessage = retrofitException.getCause().getLocalizedMessage();

    String type = this.getTypeOfNetworkError(cause, localizedMessage);
    assertEquals(type, "UNKOWN_IOEXCEPTION_WHILE_SERVER_COMMUNICATION");

    Map<String, String> responseBodyMap = retrofitException.getBodyAs(HashMap.class);
    assertNull(responseBodyMap);
  }

  private String getTypeOfNetworkError(String cause, String localizedMessage) {

    // use of the message.
    String type = "UNKOWN_IOEXCEPTION_WHILE_SERVER_COMMUNICATION";
    String[] array1 = {
      "Unable to resolve host",
      "Invalid sequence",
      "Illegal character",
      "was not verified",
      "Connection closed by peer",
      "host == null",
      "CertPathValidatorException"
    };
    String[] array2 = {"timeout", "timed out", "failed to connect", "was not verified"};
    if (cause == null && localizedMessage == null) {
      type = "ERROR_NO_RESPONSE";
    } else if (Arrays.asList(array1).contains(cause)) {
      type = "ERROR_UNABLE_TO_RESOLVE_HOST";
    } else if (cause.contains("Network unavailable")) {
      type = "ERROR_CONNECTION_OFFLINE";
    } else if (Arrays.asList(array2).contains(cause)) {
      type = "ERROR_CONNECTION_TIMEOUT";
    }
    return type;
  }

  @Test
  public void testOrderOfConverterFactory() throws JsonProcessingException {
    // Order of adding the converter factories while retrofit2 building is important.
    // It gives com.fasterxml.jackson.core.io.JsonEOFException if we use a JacksonConverterFactory.
    // But if we add GsonConverterFactory, it gives a more detailed
    // com.google.gson.stream.MalformedJsonException.
    // Retrofit2 parses first with GsonConverterFactory then JacksonConverterFactory.

    // Intentionally remove last 5 characters to make it an invalid json.
    String invalidContentJsonBodyString = "{\"name\":\"te";
    Map<String, String> expectedContentBodyMap = new HashMap<>();
    expectedContentBodyMap.put("name", "test");

    // Create a GsonConverterFactory to parse the json body from the RetrofitException into a
    // HashMap.
    List<Converter.Factory> converterFactoryList1 = new ArrayList<>();
    // Use GsonConverterFactory uses lenient option to allow invalid json strings also.
    com.google.gson.Gson gson = new GsonBuilder().setLenient().create();
    converterFactoryList1.add(GsonConverterFactory.create(gson));
    converterFactoryList1.add(JacksonConverterFactory.create());
    try {
      this.testApiServiceForJsonResponse(
          expectedContentBodyMap, invalidContentJsonBodyString, converterFactoryList1);
    } catch (Exception exception) {
      assertTrue(exception.getMessage().contains("com.google.gson.stream.MalformedJsonException"));
    }

    // Now reverse the order of factories in the list by adding gson after jackson.
    List<Converter.Factory> converterFactoryList2 = new ArrayList<>();
    converterFactoryList2.add(JacksonConverterFactory.create());
    converterFactoryList2.add(GsonConverterFactory.create(gson));
    try {
      this.testApiServiceForJsonResponse(
          expectedContentBodyMap, invalidContentJsonBodyString, converterFactoryList2);
    } catch (Exception exception) {
      assertTrue(exception.getMessage().contains("com.fasterxml.jackson.core.io.JsonEOFException"));
    }
  }

  interface Retrofit2Service {
    @retrofit2.http.GET("/retrofit2")
    Call<String> foo();
  }
}
