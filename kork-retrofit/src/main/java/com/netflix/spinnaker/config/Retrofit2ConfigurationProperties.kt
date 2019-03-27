package com.netflix.spinnaker.config

import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "retrofit2")
class Retrofit2ConfigurationProperties {
  var logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC
}
