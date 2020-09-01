/*
 * Copyright 2020 Armory
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

package com.netflix.spinnaker.credentials.definition;

import com.netflix.spinnaker.credentials.Credentials;
import com.netflix.spinnaker.credentials.CredentialsLifecycleHandler;
import com.netflix.spinnaker.credentials.CredentialsRepository;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.Builder;

@Builder
@NonnullByDefault
public class CredentialsDefinitionSourceDescriptor<
    T extends CredentialsDefinition, U extends Credentials> {
  private final CredentialsParser<T, U> parser;
  private final Supplier<List<T>> springAccountSource;
  private final CredentialsLifecycleHandler<U> eventHandler;
  @Nullable private final CredentialsDefinitionSource<T> customAccountSource;
  private final CredentialsRepository<U> credentialsRepository;

  public AbstractCredentialsLoader<U> getCredentialsLoader() {
    if (customAccountSource == null) {
      return new BasicCredentialsLoader<>(springAccountSource::get, parser, credentialsRepository);
    }
    return new BasicCredentialsLoader<>(customAccountSource, parser, credentialsRepository);
  }
}
