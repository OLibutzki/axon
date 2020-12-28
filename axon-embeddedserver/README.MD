# Embedded Server
The Embedded Server is the equivalent of the Axon Server. It acts as a global event store and is capable of dispatching commands and queries.

In contrast to the Axon Server the EmbeddedServer API does not rely on remote calls.

This module represents the server side of the Embdedded Server, so basically it plays the same role as the remote Axon Server instance.

## Public interface
The only public interface to interact with is `EmbeddedServer`. It provides methods which are similar to the mthods exposed by `EventStore`, `CommandBus` and `QueryBus`. It's possible to register/subscribe handlers and to publish commands, queries and events.


## Providing 'raw' event data
In contrast to the `EventStore` the `EmbeddedServer` does not return instances of `org.axonframework.messaging.Message`, but of type `org.axonframework.eventhandling.EventData`.

`Message` is the result of applying the upcaster chain. Upcasters are registered at the event consuming side and therefore the consuming side needs the "raw data" (`EventData`).

## JPA based
While the public `EmbeddedServer` interface is agnostic of the chosen EventSore-Implementation only supports `EmbeddedServerJpaEventStorageEngine`. It shouldn't be that hard to extend this to other implementations, but that's not part of this first draft.

## Spring Boot integration
The easiest way to configure the Embedded Server is by using Spring Boot as the modules provides an Auto-Configuration.

The configuration is enabled if it's on the classpath and `axon.embeddedserver.enabled` is not set to `false` explictly.

The `AxonEmbeddedServerConfiguration` is bootstrapped in a sperate spring application which is part of a context hierarchy. The `ExportingBeanPostProcessor` exports the `EmbeddedServer` implementation to the parent context in order to make it accessible by clients.

## Client-side
The corresponding client-side implementation is located in the `axon-embeddedserver-connector` module.

Please consult the [docs](../axon-embeddedserver-connector/README.MD).