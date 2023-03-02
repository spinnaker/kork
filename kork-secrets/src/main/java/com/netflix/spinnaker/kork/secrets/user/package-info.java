/*
 * Copyright 2023 Apple Inc.
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

/**
 * {@linkplain com.netflix.spinnaker.kork.secrets.user.UserSecret User secrets} are a specialized
 * form of {@linkplain com.netflix.spinnaker.kork.secrets.EncryptedSecret external secrets} with
 * {@linkplain com.netflix.spinnaker.kork.secrets.user.UserSecretMetadata metadata} defining
 * authorized roles who may use the {@linkplain com.netflix.spinnaker.kork.secrets.user.UserSecret
 * secret} along with {@linkplain com.netflix.spinnaker.kork.secrets.user.UserSecretSerde format
 * encoding info}.
 *
 * @see com.netflix.spinnaker.kork.secrets.user.UserSecret
 * @see com.netflix.spinnaker.kork.secrets.user.UserSecretReference
 * @see com.netflix.spinnaker.kork.secrets.user.UserSecretManager
 * @see com.netflix.spinnaker.kork.secrets.user.UserSecretService
 */
@NonnullByDefault
package com.netflix.spinnaker.kork.secrets.user;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
