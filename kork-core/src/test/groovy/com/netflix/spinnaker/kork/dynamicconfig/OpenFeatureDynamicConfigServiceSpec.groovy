/*
 * Copyright 2024 Apple, Inc.
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

package com.netflix.spinnaker.kork.dynamicconfig

import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification
import spock.lang.Subject

class OpenFeatureDynamicConfigServiceSpec extends Specification {
  def environment = new MockEnvironment()
    .withProperty('flag.enabled', 'true')
    .withProperty('some.string', 'hello')
    .withProperty('some.int', '42')
  def fallbackDynamicConfigService = new SpringDynamicConfigService(environment: environment)
  def flags = [
    'bool.enabled': Flag.builder()
      .variant('on', true)
      .variant('off', false)
      .defaultVariant('on')
      .build(),
    'color'       : Flag.builder()
      .variant('red', 0xff0000)
      .variant('green', 0x00ff00)
      .variant('blue', 0x0000ff)
      .defaultVariant('blue')
      .build()
  ]
  def api = OpenFeatureAPI.instance.tap { setProviderAndWait(new InMemoryProvider(flags)) }

  @Subject
  def service = new OpenFeatureDynamicConfigService(api.client, fallbackDynamicConfigService)

  def "can resolve basic flags"() {
    expect:
    service.isEnabled('flag', false)
    service.isEnabled('bool', false)
  }

  def "can resolve basic configs"() {
    expect:
    service.getConfig(Integer, 'color', 0) == 0xff
    service.getConfig(Integer, 'some.int', 0) == 42
    service.getConfig(String, 'some.string', '') == 'hello'
  }
}
