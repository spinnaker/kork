package com.netflix.spinnaker.kork.retrofit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.netflix.spinnaker.config.DefaultServiceEndpoint;
import com.netflix.spinnaker.config.okhttp3.OkHttpClientProvider;
import com.netflix.spinnaker.kork.retrofit.exceptions.SpinnakerHttpException;
import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties;
import com.netflix.spinnaker.okhttp.SpinnakerRequestInterceptor;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;

class RetrofitServiceFactoryTest {
  public interface RestService {
    @GET("/")
    Response exec();
  }

  @Test
  void TestUnwrappedException() {
    WireMockServer wireMockServer =
        new WireMockServer(options().dynamicPort().notifier(new Slf4jNotifier(false)));
    try {
      wireMockServer.start();
      // Exceptions returned by any service will get converted to a retrofit
      // error from the client's retrofit hooks. So, the expectation here is
      // that we expect to get the original exception back and not some nested
      // retrofit error.
      String body =
          "{\"timestamp\":\"2023-10-16T22:04:22.207+00:00\",\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"random exception\"}";
      wireMockServer.stubFor(
          WireMock.any(UrlPattern.ANY)
              .inScenario("Retry Scenario")
              .willReturn(
                  aResponse()
                      .withStatus(500)
                      .withHeader("Content-Type", "application/json")
                      .withBody(body)
                      .withFixedDelay(100)));

      OkHttpClientProvider clientProvider = Mockito.mock(OkHttpClientProvider.class);
      Mockito.when(clientProvider.getClient(Mockito.any())).thenReturn(new OkHttpClient());
      RetrofitServiceFactory factory =
          new RetrofitServiceFactory(
              RestAdapter.LogLevel.NONE,
              clientProvider,
              new SpinnakerRequestInterceptor(new OkHttpClientConfigurationProperties()));
      RestService service =
          factory.create(
              RestService.class,
              new DefaultServiceEndpoint("testUnwrappedExceptionClient", wireMockServer.baseUrl()),
              new ObjectMapper());

      Response resp = service.exec();
      fail("HTTP request should have failed, but succeeded");
    } catch (Exception e) {
      assertTrue(e instanceof SpinnakerHttpException);
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage().contains("random exception"));
    } finally {
      wireMockServer.stop();
    }
  }
}
