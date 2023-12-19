/*
 * Copyright 2023 Apple Inc.
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

package com.netflix.spinnaker.credentials.constraint;

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.kork.plugins.api.internal.SpinnakerExtensionPoint;
import javax.validation.ConstraintValidator;

/**
 * Validates a {@link CredentialsDefinition} instance. Beans of this type are used when validating
 * the {@link ValidatedCredentials} constraint annotation.
 */
public interface CredentialsDefinitionValidator
    extends SpinnakerExtensionPoint,
        ConstraintValidator<ValidatedCredentials, CredentialsDefinition> {}
