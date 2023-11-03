/*
 * Copyright 2020 Netflix, Inc.
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
 *
 */

package com.netflix.spinnaker.kork.retrofit

import brave.Tracing
import brave.http.HttpTracing
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.config.DefaultServiceEndpoint
import com.netflix.spinnaker.config.RetrofitConfiguration
import com.netflix.spinnaker.config.okhttp3.DefaultOkHttpClientBuilderProvider
import com.netflix.spinnaker.config.okhttp3.OkHttpClientProvider
import com.netflix.spinnaker.config.okhttp3.RawOkHttpClientFactory
import com.netflix.spinnaker.config.DefaultServiceClientProvider
import com.netflix.spinnaker.config.OkHttpClientComponents
import com.netflix.spinnaker.kork.client.ServiceClientFactory
import com.netflix.spinnaker.kork.client.ServiceClientProvider
import com.netflix.spinnaker.kork.common.Header
import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties
import com.netflix.spinnaker.okhttp.SpinnakerRequestHeaderInterceptor
import com.netflix.spinnaker.security.AuthenticatedRequest
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockkStatic
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit.Callback
import retrofit.http.GET
import retrofit.http.Path
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.util.Optional
import java.util.concurrent.TimeUnit

class RetrofitServiceProviderTest  : JUnit5Minutests {

  fun tests() = rootContext {
    derivedContext<ApplicationContextRunner>("no configuration") {
      fixture {
        ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(
            RetrofitServiceFactoryAutoConfiguration::class.java,
            TaskExecutionAutoConfiguration::class.java,
            OkHttpClientComponents::class.java,
            RetrofitConfiguration::class.java,
            TestConfiguration::class.java
          ))
      }

      test("initializes service client provider") {
        run { ctx: AssertableApplicationContext ->
          expect {
            that(ctx.getBeansOfType(ServiceClientProvider::class.java)).get { size }.isEqualTo(1)
            that(ctx.getBean(ServiceClientProvider::class.java).getService(Retrofit1Service::class.java, DefaultServiceEndpoint("retrofit1", "https://www.test.com"))).isA<Retrofit1Service>()
          }
        }
      }

      test("initializes service client factories") {
        run { ctx: AssertableApplicationContext ->
          expect {
            that(ctx.getBeansOfType(ServiceClientFactory::class.java)).get { size }.isEqualTo(1)
          }
        }
      }

      test("auth headers are propagated once") {
        mockkStatic(AuthenticatedRequest::class)
        every { AuthenticatedRequest.getAuthenticationHeaders() } returns mapOf(
          Header.USER.header to Optional.of("user"),
        )

        run { ctx: AssertableApplicationContext ->
          val mockWebServer = MockWebServer()
          mockWebServer.enqueue(MockResponse().setBody("response"))

          val retrofitClient = ctx.getBean(ServiceClientProvider::class.java).getService(
            Retrofit1Service::class.java,
            DefaultServiceEndpoint("retrofit1", mockWebServer.url("/").toString())
          )

          retrofitClient.getSomething("user", null)
          val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)

          expect {
            that(recordedRequest).isNotNull()
            that(recordedRequest!!.headers.values("X-SPINNAKER-USER").size).isEqualTo(1)
          }

          mockWebServer.shutdown()
        }
      }
    }
  }

}

@Configuration
private open class TestConfiguration {

  @Bean
  open fun httpTracing(): HttpTracing =
    HttpTracing.newBuilder(Tracing.newBuilder().build()).build()

  @Bean
  open fun okHttpClient(
    httpTracing: HttpTracing,
    spinnakerRequestHeaderInterceptor: SpinnakerRequestHeaderInterceptor,
  ): OkHttpClient {
    return RawOkHttpClientFactory().create(
      OkHttpClientConfigurationProperties(),
      listOf(spinnakerRequestHeaderInterceptor),
      httpTracing,
    )
  }

  @Bean
  open fun okHttpClientProvider(okHttpClient: OkHttpClient): OkHttpClientProvider {
    return OkHttpClientProvider(listOf(DefaultOkHttpClientBuilderProvider(okHttpClient, OkHttpClientConfigurationProperties())))
  }

  @Bean
  open fun objectMapper(): ObjectMapper {
    return  ObjectMapper()
  }

  @Bean
  open fun serviceClientProvider(
    serviceClientFactories: List<ServiceClientFactory?>, objectMapper: ObjectMapper): DefaultServiceClientProvider {
    return DefaultServiceClientProvider(serviceClientFactories, objectMapper)
  }
}

interface Retrofit1Service {

  @GET("/users/{user}/something")
  fun getSomething(@Path("user") user: String?, callback: Callback<List<*>?>?)

}


