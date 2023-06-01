/*
 * Copyright 2023 OpsMx, Inc.
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

package com.netflix.spinnaker.kork.retrofit.exceptions;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@link Converter.Factory converter} utility class in Retrofit2 which converts an HTTP response
 * body to a byte array. It is used when the response from the server is expected to be a binary
 * data such as an image or a file. It is added it to the Retrofit2 instance using the
 * addConverterFactory method. It does not support request body conversion, only response body
 * conversion.
 */
public class ByteArrayConverterFactoryTestUtil extends Converter.Factory {
  /**
   * ByteArrayConverterFactoryTestUtil class has a private constructor and a public `create()`
   * method that returns a new instance of the class.
   */
  private ByteArrayConverterFactoryTestUtil() {}

  /**
   * Create an instance of the {@link ByteArrayConverterFactoryTestUtil}.
   *
   * @return the new instance.
   */
  public static ByteArrayConverterFactoryTestUtil create() {
    return new ByteArrayConverterFactoryTestUtil();
  }

  /**
   * The `responseBodyConverter()` method is overridden to return a `Converter` that converts a
   * `ResponseBody` to a byte array. It checks if the `Type` is `byte[]` and
   *
   * @param type
   * @param annotations
   * @param retrofit
   * @return
   */
  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (type == byte[].class) {
      return new Converter<ResponseBody, byte[]>() {
        @Override
        public byte[] convert(ResponseBody value) throws IOException {
          // server side returns 200 with empty body, then convert() would throw an Exception.
          // Expect null or empty body from Retrofit, hence add null check for an empty pojo which
          // is {} in JSON.
          if (value.contentLength() == 0) return null;
          return value.bytes();
        }
      };
    }
    return null;
  }
}
