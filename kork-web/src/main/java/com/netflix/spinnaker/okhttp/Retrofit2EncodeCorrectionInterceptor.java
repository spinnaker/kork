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

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class Retrofit2EncodeCorrectionInterceptor implements Interceptor {

  private final boolean skipEncodingCorrection;

  public Retrofit2EncodeCorrectionInterceptor() {
    this.skipEncodingCorrection = false;
  }

  public Retrofit2EncodeCorrectionInterceptor(boolean skipEncodingCorrection) {
    this.skipEncodingCorrection = skipEncodingCorrection;
  }

  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    if (skipEncodingCorrection) {
      return chain.proceed(chain.request());
    }

    Request originalRequest = chain.request();
    HttpUrl originalUrl = originalRequest.url();
    HttpUrl.Builder newUrlBuilder = originalUrl.newBuilder();

    // Decode and encode the path to correct the partial encoding done by retrofit2
    for (int i = 0; i < originalUrl.pathSize(); i++) {
      String retrofit2EncodedSegment = originalUrl.encodedPathSegments().get(i);
      retrofit2EncodedSegment = processRetrofit2EncodedString(retrofit2EncodedSegment);
      newUrlBuilder.setEncodedPathSegment(i, retrofit2EncodedSegment);
    }

    // Decode and encode the query parameters to correct the partial encoding done by retrofit2
    for (String paramName : originalUrl.queryParameterNames()) {
      String retrofit2EncodedParam = getEncodedQueryParam(originalUrl, paramName);
      if (retrofit2EncodedParam != null) {
        String encodedParam = processRetrofit2EncodedString(retrofit2EncodedParam);
        newUrlBuilder.setEncodedQueryParameter(paramName, encodedParam);
      }
    }

    Request newRequest = originalRequest.newBuilder().url(newUrlBuilder.build()).build();

    return chain.proceed(newRequest);
  }

  /** Extracts the encoded value of a query parameter from the full encodedQuery string. */
  private String getEncodedQueryParam(HttpUrl url, String paramName) {
    String encodedQuery = url.encodedQuery();
    if (encodedQuery == null) {
      return null;
    }

    for (String pair : encodedQuery.split("&")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2 && parts[0].equals(paramName)) {
        return parts[1];
      }
    }
    return null;
  }

  /** Decodes the retrofit2-encoded value and encodes it to the correct value. */
  private String processRetrofit2EncodedString(String retrofit2EncodedString) {
    String retrofit2EncodedVal =
        retrofit2EncodedString
            .replaceAll(
                "%(?![0-9A-Fa-f]{2})",
                "%25") // Replace % with %25, else URLDecoder.decode will fail
            .replaceAll(
                "\\+",
                "%2B"); // this is needed to preserve +, else URLDecoder.decode will replace + with
    // space
    String decodedVal = URLDecoder.decode(retrofit2EncodedVal, StandardCharsets.UTF_8);
    String encodedString = URLEncoder.encode(decodedVal, StandardCharsets.UTF_8);
    return encodedString.replace("+", "%20"); // encoding replaces space with +
  }
}
