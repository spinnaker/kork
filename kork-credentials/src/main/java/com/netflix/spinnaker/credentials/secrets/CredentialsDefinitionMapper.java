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

package com.netflix.spinnaker.credentials.secrets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.spinnaker.credentials.CredentialsError;
import com.netflix.spinnaker.credentials.CredentialsView;
import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import com.netflix.spinnaker.credentials.definition.CredentialsType;
import com.netflix.spinnaker.credentials.validator.CredentialsDefinitionErrorCode;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.user.UserSecretReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

/**
 * Maps credential definitions to and from strings. Only {@link CredentialsDefinition} classes
 * annotated with a {@link CredentialsType} will be considered. {@link UserSecretReference} URIs may
 * be used for credentials values which will be replaced with an appropriate string for the secret
 * along with recording an associated account name for time of use permission checks on the user
 * secret.
 */
@Component
@NonnullByDefault
public class CredentialsDefinitionMapper {

  private final ObjectMapper objectMapper;
  private final CredentialsDefinitionSecretManager secretManager;
  private final List<Validator> validators;

  public CredentialsDefinitionMapper(
      ObjectMapper objectMapper,
      CredentialsDefinitionSecretManager secretManager,
      ObjectProvider<Validator> validators) {
    this.objectMapper = objectMapper;
    this.secretManager = secretManager;
    this.validators =
        validators.stream()
            .filter(validator -> validator.supports(CredentialsDefinition.class))
            .collect(Collectors.toList());
  }

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
   */
  public CredentialsDefinition deserializeWithSecrets(String string)
      throws JsonProcessingException {
    JsonNode jsonNode = objectMapper.readTree(string);
    if (!jsonNode.isObject()) {
      throw new SpinnakerException("Expected an object node but got " + jsonNode.getNodeType());
    }
    ObjectNode account = (ObjectNode) jsonNode;
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
                  .map(ref -> secretManager.getUserSecretString(ref, accountName));
        } else if (EncryptedSecret.isEncryptedSecret(text)) {
          plaintext = EncryptedSecret.tryParse(text).map(secretManager::getExternalSecretString);
        } else {
          plaintext = Optional.empty();
        }

        plaintext.map(account::textNode).ifPresent(field::setValue);
      }
    }
    return objectMapper.convertValue(account, CredentialsDefinition.class);
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
      view.setSpec(Map.of("data", string));
      view.getStatus()
          .setErrors(
              List.of(
                  new CredentialsError(
                      CredentialsDefinitionErrorCode.INVALID_SYNTAX.getErrorCode(),
                      e.getMessage())));
      return view;
    }

    view.setSpec(jsonNode);

    if (!jsonNode.isObject()) {
      view.getStatus()
          .setErrors(
              List.of(
                  new CredentialsError(
                      CredentialsDefinitionErrorCode.INVALID_STRUCTURE.getErrorCode(),
                      "Expected an object node but instead got " + jsonNode.getNodeType())));
      return view;
    }

    // ensure we have a `name` and `type` field
    ObjectNode account = (ObjectNode) jsonNode;
    Errors errors = new BeanPropertyBindingResult(account, "definition");
    if (!account.hasNonNull("name")) {
      errors.rejectValue(
          "name",
          CredentialsDefinitionErrorCode.MISSING_NAME.getErrorCode(),
          "No 'name' field in credentials definition");
    } else {
      view.getMetadata().setName(account.get("name").asText());
    }
    if (!account.hasNonNull("type")) {
      errors.rejectValue(
          "type",
          CredentialsDefinitionErrorCode.MISSING_TYPE.getErrorCode(),
          "No 'type' field in credentials definition");
    } else {
      view.getMetadata().setType(account.get("type").asText());
    }

    try {
      CredentialsDefinition loaded =
          objectMapper.convertValue(account, CredentialsDefinition.class);
      view.setSpec(loaded);
      validators.forEach(validator -> validator.validate(loaded, errors));
    } catch (IllegalArgumentException e) {
      errors.reject(CredentialsDefinitionErrorCode.INVALID_BINDING.getErrorCode(), e.getMessage());
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
      view.getStatus().setErrors(errorList);
    } else {
      // currently valid until proven otherwise (such as a duplicate account)
      view.getStatus().setValid(true);
    }

    return view;
  }
}
