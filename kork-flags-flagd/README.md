# flagd OpenFeature Provider

Spinnaker provides integration with [flagd](https://flagd.dev), an open source feature flag daemon that integrates with [OpenFeature](https://openfeature.dev).
This module sets up a flagd `FeatureProvider` bean as required by `kork-flags` to be used in `OpenFeatureAPI`.
This module supports extensions which can poll for or otherwise handle feature flag configuration update notifications to inform flagd to load the updated flags.

## Configuration

* `flags.providers.flagd.enabled`: toggles whether this provider is enabled.
* `flags.providers.flagd.offline.enabled`: toggles offline mode; if enabled, requires a flag source path to be defined.
* `flags.providers.flagd.offline.flag-source-path`: path to source of feature flags to periodically reload.
* `flags.providers.flagd.sync.enabled`: toggles sync mode; if enabled, sets up an in-process gRPC provider for the flagd `FlagSyncService`. Extensions should emit a `SyncFlagsEvent` event on startup and as needed to inform flagd of the current flags configuration.
* `flags.providers.flagd.sync.socket-path`: if defined, uses a unix socket for gRPC communication (requires platform support for Netty's epoll API)
* `flags.providers.flagd.sync.port`: specifies the local port to use for the gRPC service (default is 8015). Note that this port binding is made to a loopback interface.
