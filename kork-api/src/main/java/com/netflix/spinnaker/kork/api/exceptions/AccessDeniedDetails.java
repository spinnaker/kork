package com.netflix.spinnaker.kork.api.exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Details regarding an access denied exception.
 *
 * <p>TODO(jonsie): We need to migrate fiat-api into a kork module (like kork-authz) so that we can
 * create a proper fiat-api module which would provide things like Fiat's Authorization and
 * ResourceType objects. For now, this object just uses strings to represent resource type and
 * authorization, but eventually should support types provided from fiat-api.
 */
public class AccessDeniedDetails implements ExceptionDetails {
  @Nonnull private final String resourceType;
  @Nonnull private final String resourceName;
  @Nullable private final String authorization;

  public AccessDeniedDetails(String resourceType, String resourceName, String authorization) {
    this.resourceType = resourceType;
    this.resourceName = resourceName;
    this.authorization = authorization;
  }

  @Nonnull
  public String getResourceType() {
    return resourceType;
  }

  @Nonnull
  public String getResourceName() {
    return resourceName;
  }

  @Nullable
  public String getAuthorization() {
    return authorization;
  }

  public boolean hasAuthorization() {
    return authorization != null;
  }
}
