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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
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
  public void testResponseBodyWithNullContent() {
    String nullobj = null;
    assertThrows(
        NullPointerException.class,
        () ->
            ResponseBody.create(MediaType.parse("application/json" + "; charset=utf-8"), nullobj));
  }

  @Test
  public void testResponseWithNullResponseBody() {
    assertThrows(NullPointerException.class, () -> Response.error(404, null));
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

    assertThrows(RuntimeException.class, () -> retrofitException.getBodyAs(HashMap.class));
  }

  @Test
  public void testNetworkError() {
    RetrofitException retrofitException =
        RetrofitException.networkError(new IOException("io exception occured"));
    Map<String, String> responseBody = retrofitException.getBodyAs(HashMap.class);
    assertEquals(null, responseBody);
  }

  @Test
  public void testServerException() {
    MediaType.get("application/json; charset=utf-8");
    RetrofitException retrofitException =
        RetrofitException.unexpectedError(new NullPointerException("np"));
    Map<String, String> responseBody = retrofitException.getBodyAs(HashMap.class);
    assertEquals(null, responseBody);
  }
}
