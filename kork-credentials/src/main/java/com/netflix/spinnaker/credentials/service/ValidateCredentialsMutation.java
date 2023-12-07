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
 *
 */

package com.netflix.spinnaker.credentials.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(ValidateCredentialsMutation.Create.class),
  @JsonSubTypes.Type(ValidateCredentialsMutation.Update.class),
  @JsonSubTypes.Type(ValidateCredentialsMutation.Save.class),
  @JsonSubTypes.Type(ValidateCredentialsMutation.Delete.class)
})
public interface ValidateCredentialsMutation {
  @Value
  @Builder
  @Jacksonized
  @JsonTypeName("create")
  class Create implements ValidateCredentialsMutation {
    @Valid @NotNull CredentialsDefinition credentials;
  }

  @Value
  @Builder
  @Jacksonized
  @JsonTypeName("update")
  class Update implements ValidateCredentialsMutation {
    @Valid @NotNull CredentialsDefinition credentials;
  }

  @Value
  @Builder
  @Jacksonized
  @JsonTypeName("save")
  class Save implements ValidateCredentialsMutation {
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Singular
    @NotEmpty
    List<@Valid @NotNull CredentialsDefinition> credentials;
  }

  @Value
  @Builder
  @Jacksonized
  @JsonTypeName("delete")
  class Delete implements ValidateCredentialsMutation {
    @Singular @NotEmpty List<@NotBlank String> credentials;
  }
}
