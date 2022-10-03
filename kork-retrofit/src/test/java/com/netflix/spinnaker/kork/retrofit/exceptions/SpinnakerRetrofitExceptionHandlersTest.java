/*
 * Copyright 2021 Salesforce, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.retrofit.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import com.netflix.spinnaker.config.ErrorConfiguration;
import com.netflix.spinnaker.config.RetrofitErrorConfiguration;
import com.netflix.spinnaker.kork.test.log.MemoryAppender;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import retrofit.client.Response;
import retrofit.mime.TypedString;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      ErrorConfiguration.class,
      RetrofitErrorConfiguration.class,
      SpinnakerRetrofitExceptionHandlersTest.TestControllerConfiguration.class
    })
@TestPropertySource(properties = {"retrofit.enabled = false"})
class SpinnakerRetrofitExceptionHandlersTest {

  private static final String CUSTOM_MESSAGE = "custom message";

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  private MemoryAppender memoryAppender;

  @BeforeEach
  private void setup(TestInfo testInfo) {
    System.out.println("--------------- Test " + testInfo.getDisplayName());
    memoryAppender = new MemoryAppender(SpinnakerRetrofitExceptionHandlers.class);
  }

  @Test
  void testSpinnakerServerException() throws Exception {
    URI uri = getUri("/spinnakerServerException");

    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
    assertEquals(1, memoryAppender.countEventsForLevel(Level.ERROR));
  }

  @Test
  void testChainedSpinnakerServerException() throws Exception {
    URI uri = getUri("/chainedSpinnakerServerException");

    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
    assertEquals(1, memoryAppender.countEventsForLevel(Level.ERROR));

    // Make sure the message is what we expect.
    assertEquals(1, memoryAppender.search(CUSTOM_MESSAGE, Level.ERROR).size());
  }

  @ParameterizedTest(name = "testSpinnakerHttpException status = {0}")
  @ValueSource(ints = {403, 400, 500})
  void testSpinnakerHttpException(int status) throws Exception {
    URI uri = getUri("/spinnakerHttpException/" + String.valueOf(status));

    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(status, entity.getStatusCode().value());

    // Only expect error logging for a server error, debug otherwise.  No need
    // to fill up logs with client errors assuming the server is doing the best
    // it can.
    assertEquals(
        1,
        memoryAppender.countEventsForLevel(
            HttpStatus.resolve(status).is5xxServerError() ? Level.ERROR : Level.DEBUG));
  }

  @ParameterizedTest(name = "testChainedSpinnakerHttpException status = {0}")
  @ValueSource(ints = {403, 400, 500})
  void testChainedSpinnakerHttpException(int status) throws Exception {
    URI uri = getUri("/chainedSpinnakerHttpException/" + String.valueOf(status));

    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(status, entity.getStatusCode().value());

    // Only expect error logging for a server error, debug otherwise.  No need
    // to fill up logs with client errors assuming the server is doing the best
    // it can.
    assertEquals(
        1,
        memoryAppender.countEventsForLevel(
            HttpStatus.resolve(status).is5xxServerError() ? Level.ERROR : Level.DEBUG));

    // Make sure the message is what we expect.
    assertEquals(
        1,
        memoryAppender
            .search(
                CUSTOM_MESSAGE,
                HttpStatus.resolve(status).is5xxServerError() ? Level.ERROR : Level.DEBUG)
            .size());
  }

  private URI getUri(String path) {
    return UriComponentsBuilder.fromHttpUrl("http://localhost/test-controller")
        .port(port)
        .path(path)
        .build()
        .toUri();
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestControllerConfiguration {
    @EnableWebSecurity
    class WebSecurityConfig extends WebSecurityConfigurerAdapter {
      @Override
      protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().headers().disable().authorizeRequests().anyRequest().permitAll();
      }
    }

    @Bean
    TestController testController() {
      return new TestController();
    }
  }

  @RestController
  @RequestMapping("/test-controller")
  static class TestController {

    @GetMapping("/spinnakerServerException")
    void spinnakerServerException() {
      // Building a "real" RetrofitError object to pass to the
      // SpinnakerServerException constructor isn't great, e.g.:
      //
      // String url = "https://some-url";
      // Response response = new Response(url, HttpStatus.BAD_GATEWAY.value(), "arbitrary reason",
      // List.of(), new TypedString("{ message: \"message\" }"))
      // RetrofitError retrofitError = RetrofitError.httpError(url, response,
      // Platform.get().defaultConverter(), Response.class)
      //
      // and neither is a mock RetrofitError object since it means exposing
      // SpinnakerServerException.RetrofitErrorResponseBody, e.g:
      //
      // RetrofitError retrofitError = mock(RetrofitError.class)
      // String message = "message";
      // SpinnakerServerException.RetrofitErrorResponseBody retrofitErrorResponseBody = new
      // SpinnakerServerException.RetrofitErrorResponseBody(message);
      // when(retrofitError.getBodyAs(any())).thenReturn retrofitErrorResponseBody;
      // throw new SpinnakerServerException(retrofitError)
      //
      // And in the end, the thing we care about is how SpinnakerServerException
      // gets handled.  This isn't a test of how the SpinnakerServerException
      // class uses a RetrofitError to build its message.
      SpinnakerServerException spinnakerServerException = mock(SpinnakerServerException.class);
      when(spinnakerServerException.getMessage()).thenReturn("message");
      throw spinnakerServerException;
    }

    @GetMapping("/chainedSpinnakerServerException")
    void chainedSpinnakerServerException() {
      SpinnakerServerException spinnakerServerException = mock(SpinnakerServerException.class);
      when(spinnakerServerException.getMessage()).thenReturn("message");
      throw new SpinnakerServerException(CUSTOM_MESSAGE, spinnakerServerException);
    }

    @GetMapping("/spinnakerHttpException/{status}")
    void spinnakerHttpException(@PathVariable int status) {
      throw makeSpinnakerHttpException(status);
    }

    @GetMapping("/chainedSpinnakerHttpException/{status}")
    void chainedSpinnakerHttpException(@PathVariable int status) {
      throw new SpinnakerHttpException(CUSTOM_MESSAGE, makeSpinnakerHttpException(status));
    }

    SpinnakerHttpException makeSpinnakerHttpException(int status) {
      SpinnakerHttpException spinnakerHttpException = mock(SpinnakerHttpException.class);
      when(spinnakerHttpException.getMessage()).thenReturn("message");

      // Response is a final class, so straightforward mocking fails.
      String url = "https://some-url";
      Response response =
          new Response(
              url,
              status,
              "arbitrary reason",
              List.of(),
              new TypedString("{ message: \"unused message due to above mock\" }"));

      when(spinnakerHttpException.getResponse()).thenReturn(response);
      return spinnakerHttpException;
    }
  }
}
