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
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class ByteArrayConverterFactoryTestUtil extends Converter.Factory {
  private ByteArrayConverterFactoryTestUtil() {}

  public static ByteArrayConverterFactoryTestUtil create() {
    return new ByteArrayConverterFactoryTestUtil();
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {
    if (type == byte[].class) {
      return new Converter<ResponseBody, byte[]>() {
        @Override
        public byte[] convert(ResponseBody value) throws IOException {
          return value.bytes();
        }
      };
    }
    return null;
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(
      Type type,
      Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations,
      Retrofit retrofit) {
    if (type == byte[].class) {
      return new Converter<byte[], RequestBody>() {
        @Override
        public RequestBody convert(byte[] value) throws IOException {
          return RequestBody.create(MediaType.parse("application/octet-stream"), value);
        }
      };
    }
    return null;
  }
}
