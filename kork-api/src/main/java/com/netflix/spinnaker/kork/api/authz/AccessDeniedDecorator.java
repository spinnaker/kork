package com.netflix.spinnaker.kork.api.authz;

import com.netflix.spinnaker.kork.plugins.api.internal.SpinnakerExtensionPoint;

/**
 * Decorate the various authz access denied error messages that Spinnaker throws with custom
 * additional information.
 */
public interface AccessDeniedDecorator extends SpinnakerExtensionPoint {

  /** Decorates an access denied error based on the resource type and resource name. */
  String decorate(String resourceType, String resourceName);
}
