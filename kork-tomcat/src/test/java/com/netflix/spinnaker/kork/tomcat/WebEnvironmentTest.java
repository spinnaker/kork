package com.netflix.spinnaker.kork.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {WebEnvironmentTest.TestControllerConfiguration.class})
@TestPropertySource(
    properties = {"logging.level.org.apache.coyote.http11.Http11InputBuffer = DEBUG"})
class WebEnvironmentTest {

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Test
  void testTomcatWithIllegalHttpHeaders() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    // this is enough to cause a BAD_REQUEST if tomcat has rejectIllegalHeaders
    // set to true
    headers.add("X-Dum@my", "foo");

    URI uri =
        UriComponentsBuilder.fromHttpUrl("http://localhost/test-controller")
            .port(port)
            .build()
            .toUri();

    ResponseEntity<String> entity =
        restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
  }

  @SpringBootApplication
  public static class TestControllerConfiguration {
    @Bean
    TestController testController() {
      return new TestController();
    }
  }

  @RestController
  @RequestMapping("/test-controller")
  static class TestController {

    @GetMapping
    void get() {
      return;
    }
  }
}
