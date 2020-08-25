package com.netflix.spinnaker.kork.plugins.remote.extension

interface RemoteExtensionConfigType

/**
 * No-op in the case wherein a remote extension does not have any necessary configuration.
 */
data class NoOpRemoteExtensionConfigType(val type: String = "noop"): RemoteExtensionConfigType
