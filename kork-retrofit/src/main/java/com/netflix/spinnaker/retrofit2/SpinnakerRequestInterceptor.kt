package com.netflix.spinnaker.retrofit2

import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties
import com.netflix.spinnaker.security.AuthenticatedRequest
import okhttp3.Interceptor
import okhttp3.Response

class SpinnakerRequestInterceptor(
  private val okHttpClientConfigurationProperties: OkHttpClientConfigurationProperties
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val requestBuilder = chain.request().newBuilder()
    if (okHttpClientConfigurationProperties.propagateSpinnakerHeaders) {
      AuthenticatedRequest
        .getAuthenticationHeaders()
        .forEach { (k, v) ->
          v.ifPresent {
            requestBuilder.addHeader(k, it)
          }
        }
    }
    return chain.proceed(requestBuilder.build())
  }
}
