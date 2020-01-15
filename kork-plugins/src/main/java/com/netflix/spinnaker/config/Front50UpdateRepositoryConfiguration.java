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
 */

package com.netflix.spinnaker.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.netflix.spinnaker.kork.plugins.config.ConfigResolver;
import com.netflix.spinnaker.kork.plugins.config.RepositoryConfigCoordinates;
import com.netflix.spinnaker.kork.plugins.config.SpringEnvironmentConfigResolver;
import com.netflix.spinnaker.kork.plugins.update.front50.Front50Service;
import com.netflix.spinnaker.okhttp.OkHttpClientConfigurationProperties;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Configuration
@ConditionalOnProperty("spinnaker.extensibility.repositories.front50.enabled")
public class Front50UpdateRepositoryConfiguration {

  @Bean
  @ConditionalOnMissingBean(ConfigResolver.class)
  public static ConfigResolver springEnvironmentConfigResolver(
      ConfigurableEnvironment environment) {
    return new SpringEnvironmentConfigResolver(environment);
  }

  @Bean
  public static OkHttpClientConfigurationProperties pluginOkHttpClientProperties(
      Environment environment) {
    return Binder.get(environment)
        .bind("ok-http-client", Bindable.of(OkHttpClientConfigurationProperties.class))
        .orElseThrow(IllegalStateException::new);
  }

  @Bean
  public static OkHttpClient pluginOkHttpClient(
      OkHttpClientConfigurationProperties pluginOkHttpClientProperties) {
    return new OkHttp3ClientConfiguration(pluginOkHttpClientProperties)
        .create()
        .retryOnConnectionFailure(pluginOkHttpClientProperties.isRetryOnConnectionFailure())
        .build();
  }

  @Bean
  ObjectMapper pluginRetrofitObjectMapper() {
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Bean
  public static Front50Service pluginFront50Service(
      ConfigResolver configResolver,
      OkHttpClient pluginOkHttpClient,
      ObjectMapper pluginRetrofitObjectMapper) {

    Map<String, PluginsConfigurationProperties.PluginRepositoryProperties> repositoriesConfig =
        configResolver.resolve(
            new RepositoryConfigCoordinates(),
            new TypeReference<
                HashMap<String, PluginsConfigurationProperties.PluginRepositoryProperties>>() {});

    PluginsConfigurationProperties.PluginRepositoryProperties front50RepositoryProps =
        repositoriesConfig.get(PluginsConfigurationProperties.FRONT5O_REPOSITORY);

    return new Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(pluginRetrofitObjectMapper))
        .baseUrl(front50RepositoryProps.getUrl())
        .client(pluginOkHttpClient)
        .build()
        .create(Front50Service.class);
  }
}
