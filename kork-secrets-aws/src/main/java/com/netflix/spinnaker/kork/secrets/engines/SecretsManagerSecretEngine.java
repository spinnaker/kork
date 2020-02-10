package com.netflix.spinnaker.kork.secrets.engines;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import com.netflix.spinnaker.kork.secrets.SecretException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SecretsManagerSecretEngine implements SecretEngine {
  protected static final String SECRET_NAME = "s";
  protected static final String SECRET_REGION = "r";
  protected static final String SECRET_KEY = "k";

  private static String IDENTIFIER = "secrets-manager";

  private Map<String, Map<String, Object>> cache = new HashMap<>();

  @Override
  public String identifier() {
    return SecretsManagerSecretEngine.IDENTIFIER;
  }

  @Override
  public byte[] decrypt(EncryptedSecret encryptedSecret) {
    String secretName = encryptedSecret.getParams().get(SECRET_NAME);
    String secretRegion = encryptedSecret.getParams().get(SECRET_REGION);
    String secretKey = encryptedSecret.getParams().get(SECRET_KEY);

    AWSSecretsManager client =
        AWSSecretsManagerClientBuilder.standard().withRegion(secretRegion).build();

    byte[] binarySecret;
    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(secretName);
    GetSecretValueResult getSecretValueResult;

    try {
      getSecretValueResult = client.getSecretValue(getSecretValueRequest);
    } catch (AWSSecretsManagerException e) {
      throw new SecretException(
          String.format(
              "An error occurred when using AWS Secrets Manager to fetch: [secretName: %s, secretRegion: %s, secretKey: %s]",
              secretName, secretRegion, secretKey),
          e);
    }

    if (getSecretValueResult.getSecretString() != null) {
      String secret = getSecretValueResult.getSecretString();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode secretNode;
      try {
        secretNode = mapper.readTree(secret);
      } catch (JsonProcessingException e) {
        throw new SecretException(
            String.format(
                "Failed to parse secret when using AWS Secrets Manager to fetch: [secretName: %s, secretRegion: %s, secretKey: %s]",
                secretName, secretRegion, secretKey),
            e);
      }
      binarySecret =
          Optional.ofNullable(secretNode.get(secretKey))
              .orElseThrow(
                  () ->
                      new SecretException(
                          String.format(
                              "Specified key not found in AWS Secrets Manager: [secretName: %s, secretRegion: %s, secretKey: %s]",
                              secretName, secretRegion, secretKey)))
              .textValue()
              .getBytes(StandardCharsets.UTF_8);
    } else {
      binarySecret = getSecretValueResult.getSecretBinary().array(); // it's binary
    }
    return binarySecret;
  }

  @Override
  public void validate(EncryptedSecret encryptedSecret) {
    Set<String> paramNames = encryptedSecret.getParams().keySet();
    if (!paramNames.contains(SECRET_NAME)) {
      throw new InvalidSecretFormatException(
          "Secret name parameter is missing (" + SECRET_NAME + "=...)");
    }
    if (!paramNames.contains(SECRET_REGION)) {
      throw new InvalidSecretFormatException(
          "Secret region parameter is missing (" + SECRET_REGION + "=...)");
    }
  }

  @Override
  public void clearCache() {
    cache.clear();
  }
}
