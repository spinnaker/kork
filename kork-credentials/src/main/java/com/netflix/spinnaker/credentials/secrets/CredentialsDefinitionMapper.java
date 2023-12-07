/*
 * Copyright 2021 Apple Inc.
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

package com.netflix.spinnaker.credentials.secrets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.spinnaker.credentials.CredentialsError;
import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.jackson.ObjectNodeErrors;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import com.netflix.spinnaker.kork.secrets.user.UserSecretService;
import com.netflix.spinnaker.security.AccessControlled;
import com.netflix.spinnaker.security.SpinnakerAuthorities;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * Maps credential definitions to and from strings. Only {@link CredentialsDefinition} classes
 * annotated with a {@link CredentialsType} will be considered. {@link UserSecretReference} URIs may
 * be used for credentials values which will be replaced with an appropriate string for the secret
 * along with recording an associated account name for time of use permission checks on the user
 * secret.
 */
@Log4j2
@Component
@NonnullByDefault
@RequiredArgsConstructor
public class CredentialsDefinitionMapper {

  private final ObjectMapper objectMapper;
  private final UserSecretService secretService;

  /**
   * Serializes the given {@link CredentialsDefinition} instance into a JSON string.
   *
   * @param definition the account to serialize
   * @return the serialized account
   * @throws JsonProcessingException if there are errors converting the account into JSON
   */
  public String serialize(CredentialsDefinition definition) throws JsonProcessingException {
    return objectMapper.writeValueAsString(definition);
  }

  /**
   * Attempts to deserialize the given JSON string into an appropriate {@link CredentialsDefinition}
   * account object. This does not load or decrypt any secret URIs.
   *
   * @param string the JSON string to parse
   * @return the parsed account
   * @throws JsonProcessingException if the input string is invalid JSON or does not map to a known
   *     CredentialsDefinition class
   * @throws IllegalArgumentException if the JSON is missing a "type" or "name" field or if the JSON
   *     is not an object
   */
  public CredentialsDefinition deserialize(String string) throws JsonProcessingException {
    return objectMapper.readValue(string, CredentialsDefinition.class);
  }

  /**
   * Attempts to both deserialize and decrypt referenced secrets in the given JSON string into an
   * appropriate {@link CredentialsDefinition} account object.
   *
   * @param string the JSON string to parse
   * @return the parsed account with loaded secrets
   * @throws JsonProcessingException if the input string is invalid JSON or does not map to an
   *     existing CredentialsDefinition class
   * @throws com.netflix.spinnaker.kork.secrets.SecretException if there are errors while fetching
   *     the secret
   * @throws IllegalArgumentException if the JSON is missing a "name" field or if the JSON is not an
   *     object
   */
  public CredentialsDefinition deserializeWithSecrets(String string)
      throws JsonProcessingException {
    ObjectNode account = parseJson(string);
    String accountName = account.required("name").asText();
    Iterator<Map.Entry<String, JsonNode>> it = account.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> field = it.next();
      JsonNode node = field.getValue();
      if (node.isTextual()) {
        String text = node.asText();
        Optional<String> plaintext;
        if (UserSecretReference.isUserSecret(text)) {
          plaintext =
              UserSecretReference.tryParse(text)
                  .map(ref -> secretService.getUserSecretStringForResource(ref, accountName));
        } else if (EncryptedSecret.isEncryptedSecret(text)) {
          plaintext = EncryptedSecret.tryParse(text).map(secretService::getExternalSecretString);
        } else {
          plaintext = Optional.empty();
        }

        plaintext.map(account::textNode).ifPresent(field::setValue);
      }
    }
    return objectMapper.convertValue(account, CredentialsDefinition.class);
  }

  private ObjectNode parseJson(String string) throws JsonProcessingException {
    JsonNode jsonNode = objectMapper.readTree(string);
    if (!jsonNode.isObject()) {
      throw new IllegalArgumentException(
          "Expected an object node but got " + jsonNode.getNodeType());
    }
    return (ObjectNode) jsonNode;
  }

  /**
   * Attempts to deserialize the given JSON string into an appropriate {@link CredentialsDefinition}
   * inside a {@link CredentialsView} along with any errors encountered during deserialization. This
   * does not decrypt any secrets.
   *
   * @param string the JSON string to parse
   * @return an initialized CredentialsView with the deserialized credentials or errors
   */
  public CredentialsView deserializeWithErrors(String string) {
    CredentialsView view = new CredentialsView();

    // ensure that the JSON string is both valid JSON and an object in particular
    JsonNode jsonNode;
    try {
      jsonNode = objectMapper.readTree(string);
    } catch (JsonProcessingException e) {
      log.info("Cannot deserialize invalid JSON credentials data", e);
      view.setSpec(Map.of("data", string));
      view.getStatus()
          .addError(
              new CredentialsError(
                  CredentialsDefinitionErrorCode.INVALID_SYNTAX.getErrorCode(), e.getMessage()));
      return view;
    }

    view.setSpec(jsonNode);

    if (!jsonNode.isObject()) {
      JsonNodeType nodeType = jsonNode.getNodeType();
      log.info(
          "Encountered invalid structured JSON credentials data. Expected an object but got {}",
          nodeType);
      view.getStatus()
          .addError(
              new CredentialsError(
                  CredentialsDefinitionErrorCode.INVALID_STRUCTURE.getErrorCode(),
                  "Expected an object node but instead got " + nodeType));
      return view;
    }

    // ensure we have a `name` and `type` field
    ObjectNode account = (ObjectNode) jsonNode;
    // allow for permission checking of invalid or unknown account types
    view.setSpec(new UnknownAccount(account));
    Errors errors = new ObjectNodeErrors(account, "result");
    String name;
    String type;
    if (!account.hasNonNull("name")) {
      log.info("Missing or empty 'name' field in credentials definition");
      errors.rejectValue(
          "name",
          CredentialsDefinitionErrorCode.MISSING_NAME.getErrorCode(),
          "No 'name' field in credentials definition");
    } else {
      name = account.get("name").asText();
      ThreadContext.put("accountName", name);
      view.getMetadata().setName(name);
    }
    if (!account.hasNonNull("type")) {
      log.info("Missing or empty 'type' field in credentials definition");
      errors.rejectValue(
          "type",
          CredentialsDefinitionErrorCode.MISSING_TYPE.getErrorCode(),
          "No 'type' field in credentials definition");
    } else {
      type = account.get("type").asText();
      ThreadContext.put("accountType", type);
      log.debug("Validating account with type '{}'", type);
      view.getMetadata().setType(type);
    }

    try {
      CredentialsDefinition loaded =
          objectMapper.convertValue(account, CredentialsDefinition.class);
      view.setSpec(loaded);
    } catch (IllegalArgumentException e) {
      log.info("Invalid credentials JSON binding", e);
      errors.reject(CredentialsDefinitionErrorCode.INVALID_BINDING.getErrorCode(), e.getMessage());
    } finally {
      ThreadContext.removeAll(List.of("accountName", "accountType"));
    }

    if (errors.hasErrors()) {
      List<CredentialsError> errorList =
          errors.getAllErrors().stream()
              .map(
                  error -> {
                    String errorCode = error.getCode();
                    assert errorCode != null;
                    String message = error.getDefaultMessage();
                    assert message != null;
                    String field =
                        error instanceof FieldError ? ((FieldError) error).getField() : null;
                    return new CredentialsError(errorCode, message, field);
                  })
              .collect(Collectors.toList());
      view.getStatus().addErrors(errorList);
    }

    return view;
  }

  // provides a view into unparsed credentials to support access controls if defined
  @RequiredArgsConstructor
  private static class UnknownAccount implements AccessControlled {
    private final ObjectNode account;

    @Override
    public boolean isAuthorized(Authentication authentication, Object authorization) {
      JsonNode permittedRoles;
      JsonNode permissions = account.get("permissions");
      JsonNode artifactRoles = account.get("roles");
      JsonNode requiredGroupMembership = account.get("requiredGroupMembership");
      if (permissions != null) {
        JsonNode roles = permissions.get(authorization.toString().toUpperCase(Locale.ROOT));
        if (roles == null) {
          return true;
        }
        permittedRoles = roles;
      } else if (artifactRoles != null) {
        permittedRoles = artifactRoles;
      } else if (requiredGroupMembership != null) {
        permittedRoles = requiredGroupMembership;
      } else {
        // doesn't seem to be an access-controlled object
        return true;
      }
      if (permittedRoles.isEmpty() || !permittedRoles.isArray()) {
        // no roles defined for an authorization -> open access
        return true;
      }
      Set<String> roles = new HashSet<>();
      for (JsonNode permittedRole : permittedRoles) {
        roles.add(permittedRole.asText());
      }
      return SpinnakerAuthorities.hasAnyRole(authentication, roles);
    }
  }
}
