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

package com.netflix.spinnaker.okhttp

import com.netflix.spinnaker.security.AuthenticatedRequest
import okhttp3.Interceptor
import okhttp3.Response

/*
* As {@link retrofit.RequestInterceptor} class removed in retrofit2,
* we have to use {@link okhttp3.Interceptor} to add authenticated headers to requests.
* */
class SpinnakerRequestHeaderInterceptor implements Interceptor {

  private final OkHttpClientConfigurationProperties okHttpClientConfigurationProperties

  SpinnakerRequestHeaderInterceptor(OkHttpClientConfigurationProperties okHttpClientConfigurationProperties) {
    this.okHttpClientConfigurationProperties = okHttpClientConfigurationProperties
  }

  @Override
  Response intercept(Chain chain) throws IOException {
    def builder = chain.request().newBuilder()
    if (!okHttpClientConfigurationProperties.propagateSpinnakerHeaders) {
      // noop
      return
    }

    AuthenticatedRequest.authenticationHeaders.each { String key, Optional<String> value ->
      if (value.present) {
        builder.addHeader(key, value.get())
      }
    }

    return chain.proceed(builder.build())
  }
}
