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

package com.netflix.spinnaker.kork.dynamicconfig;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Features;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Dynamic configuration service using {@linkplain dev.openfeature.sdk.OpenFeatureAPI OpenFeature}
 * as the primary lookup mechanism with the ability to fall back to using another {@link
 * DynamicConfigService}.
 */
@Log4j2
@NonnullByDefault
@RequiredArgsConstructor
public class OpenFeatureDynamicConfigService implements DynamicConfigService {
  private final Features features;
  private final DynamicConfigService fallbackDynamicConfigService;

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getConfig(Class<T> configType, String configName, T defaultValue) {
    FlagEvaluationDetails<?> details;
    T value;
    if (configType == Boolean.class) {
      details = features.getBooleanDetails(configName, (Boolean) defaultValue);
      value = (T) details.getValue();
    } else if (configType == Integer.class) {
      details = features.getIntegerDetails(configName, (Integer) defaultValue);
      value = (T) details.getValue();
    } else if (configType == Long.class) {
      // TODO(jvz): https://github.com/open-feature/java-sdk/issues/501
      var intDetails = features.getIntegerDetails(configName, ((Long) defaultValue).intValue());
      details = intDetails;
      value = (T) Long.valueOf(intDetails.getValue().longValue());
    } else if (configType == Double.class) {
      details = features.getDoubleDetails(configName, (Double) defaultValue);
      value = (T) details.getValue();
    } else if (configType == String.class) {
      details = features.getStringDetails(configName, (String) defaultValue);
      value = (T) details.getValue();
    } else {
      var objectDetails = features.getObjectDetails(configName, Value.objectToValue(defaultValue));
      details = objectDetails;
      value = (T) objectDetails.getValue().asObject();
    }
    var errorCode = details.getErrorCode();
    if (errorCode == ErrorCode.FLAG_NOT_FOUND) {
      return fallbackDynamicConfigService.getConfig(configType, configName, defaultValue);
    }
    if (errorCode != null) {
      log.warn("Unable to resolve configuration key '{}': {}", configName, details);
    }
    return value;
  }

  @Override
  public boolean isEnabled(String flagName, boolean defaultValue) {
    var details = features.getBooleanDetails(flagPropertyName(flagName), defaultValue);
    var errorCode = details.getErrorCode();
    if (errorCode == ErrorCode.FLAG_NOT_FOUND) {
      return fallbackDynamicConfigService.isEnabled(flagName, defaultValue);
    }
    if (errorCode != null) {
      log.warn("Unable to resolve configuration flag '{}': {}", flagName, details);
    }
    return details.getValue();
  }

  @Override
  public boolean isEnabled(String flagName, boolean defaultValue, ScopedCriteria criteria) {
    var context = convertCriteria(criteria);
    var details = features.getBooleanDetails(flagPropertyName(flagName), defaultValue, context);
    var errorCode = details.getErrorCode();
    if (errorCode == ErrorCode.FLAG_NOT_FOUND) {
      return fallbackDynamicConfigService.isEnabled(flagName, defaultValue, criteria);
    }
    if (errorCode != null) {
      log.warn(
          "Unable to resolve configuration flag '{}' with {}: {}", flagName, criteria, details);
    }
    return details.getValue();
  }

  private static String flagPropertyName(String flagName) {
    return flagName.endsWith(".enabled") ? flagName : flagName + ".enabled";
  }

  private static EvaluationContext convertCriteria(ScopedCriteria criteria) {
    return new MutableContext()
        .add("region", criteria.region)
        .add("account", criteria.account)
        .add("cloudProvider", criteria.cloudProvider)
        .add("application", criteria.application);
  }
}
