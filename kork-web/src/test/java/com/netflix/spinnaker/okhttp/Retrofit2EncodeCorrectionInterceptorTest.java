/*
 * Copyright 2025 OpsMx, Inc.
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

package com.netflix.spinnaker.okhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.netflix.spinnaker.config.DefaultServiceEndpoint;
import com.netflix.spinnaker.config.ServiceEndpoint;
import com.netflix.spinnaker.config.okhttp3.DefaultOkHttpClientBuilderProvider;
import com.netflix.spinnaker.config.okhttp3.OkHttpClientProvider;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {
      OkHttpClient.class,
      OkHttpClientConfigurationProperties.class,
      OkHttpClientProvider.class,
      DefaultOkHttpClientBuilderProvider.class,
      Retrofit2EncodeCorrectionInterceptor.class
    })
public class Retrofit2EncodeCorrectionInterceptorTest {

  // to test if a path parameter value containing various special characters is handled correctly
  // with Retrofit2
  private static final String PATH_VAL = "path: [] () = % $ & # @ ; , ? \" ' end path";
  private static final String ENCODED_PATH_VAL = encodedString(PATH_VAL);

  // to test if a query parameter value containing various special characters is handled correctly
  // with Retrofit2
  private static final String QUERY_PARAM1 = "qry1: [] () + = % $ & # @ ; , ? \" ' /end qry1";
  private static final String ENCODED_QUERY_PARAM1 = encodedString(QUERY_PARAM1);

  private static final String QUERY_PARAM2 = "qry2: [] () + = % $ & # @ ; , ? \" ' end qry2";
  private static final String ENCODED_QUERY_PARAM2 = encodedString(QUERY_PARAM2);
  private static final String QUERY_PARAM3 = "";
  private static final String EXPECTED_URL =
      "/test/"
          + ENCODED_PATH_VAL
          + "/get?qry1="
          + ENCODED_QUERY_PARAM1
          + "&qry2="
          + ENCODED_QUERY_PARAM2
          + "&qry3=";
  private static ServiceEndpoint endpoint;

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired OkHttpClientProvider okHttpClientProvider;

  @BeforeAll
  public static void setup() {
    wireMock.stubFor(get(EXPECTED_URL).willReturn(ok()));
    endpoint = new DefaultServiceEndpoint("test", wireMock.baseUrl());
  }

  @Test
  public void testWithoutEncodingCorrection() throws IOException {
    OkHttpClient okHttpClient =
        okHttpClientProvider.getClient(endpoint, true /* skipEncodeCorrection */);
    Retrofit2Service service = getRetrofit2Service(endpoint.getBaseUrl(), okHttpClient);
    service.getRequest(PATH_VAL, QUERY_PARAM1, QUERY_PARAM2, QUERY_PARAM3).execute();
    LoggedRequest requestReceived = wireMock.getAllServeEvents().get(0).getRequest();
    // note the partial encoding done by Retrofit2
    assertThat(getPathFromUrl(requestReceived.getUrl()))
        .isEqualTo(
            "/test/path:%20[]%20()%20=%20%25%20$%20&%20%23%20@%20;%20,%20%3F%20%22%20'%20end%20path/get");
    wireMock.verify(0, getRequestedFor(urlEqualTo(EXPECTED_URL)));
  }

  @Test
  public void testWithEncodingCorrection() throws IOException {
    OkHttpClient okHttpClient =
        okHttpClientProvider.getClient(endpoint, false /* skipEncodeCorrection */);
    Retrofit2Service service = getRetrofit2Service(endpoint.getBaseUrl(), okHttpClient);
    service.getRequest(PATH_VAL, QUERY_PARAM1, QUERY_PARAM2, QUERY_PARAM3).execute();
    wireMock.verify(getRequestedFor(urlEqualTo(EXPECTED_URL)));
    LoggedRequest requestReceived = wireMock.getAllServeEvents().get(0).getRequest();
    assertThat(requestReceived.queryParameter("qry1").firstValue()).isEqualTo(QUERY_PARAM1);
    assertThat(requestReceived.queryParameter("qry2").firstValue()).isEqualTo(QUERY_PARAM2);
    assertThat(requestReceived.queryParameter("qry3").firstValue()).isEqualTo("");
  }

  private Retrofit2Service getRetrofit2Service(String baseUrl, OkHttpClient okHttpClient) {

    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(Retrofit2Service.class);
  }

  private static String encodedString(String input) {
    return URLEncoder.encode(input, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
  }

  private static String getPathFromUrl(String url) {
    return url.substring(0, url.indexOf('?'));
  }

  interface Retrofit2Service {
    @GET("test/{path}/get")
    Call<Void> getRequest(
        @Path("path") String path,
        @Query("qry1") String qry1,
        @Query("qry2") String qry2,
        @Query("qry3") String qry3);
  }
}
