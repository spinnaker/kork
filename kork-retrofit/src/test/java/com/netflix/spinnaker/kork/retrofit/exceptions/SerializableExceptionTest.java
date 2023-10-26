package com.netflix.spinnaker.kork.retrofit.exceptions;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedString;
import wiremock.org.apache.commons.lang3.SerializationUtils;

public class SerializableExceptionTest {
  @ParameterizedTest
  @MethodSource("parameters")
  void ensureSerializable(Function<RetrofitError, Throwable> call) {
    String url = "http://localhost";
    int statusCode = 500;
    String reason = "some reason";
    String body = "{}";
    Response response = new Response(url, statusCode, reason, List.of(), new TypedString(body));
    RetrofitError retrofitError =
        RetrofitError.httpError(url, response, new JacksonConverter(), Map.class);
    Throwable e = call.apply(retrofitError);
    SerializationUtils.roundtrip(e);
  }

  private static Stream<Function<RetrofitError, Throwable>> parameters() {
    return Stream.of(
        SpinnakerHttpException::new, SpinnakerNetworkException::new, SpinnakerServerException::new);
  }
}
