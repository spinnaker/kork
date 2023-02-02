package io.spinnaker.test.security;

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@CredentialsType("value")
@NonnullByDefault
@Value
@Builder
@Jacksonized
public class ValueAccount implements CredentialsDefinition {
  String name;
  String value;
}
