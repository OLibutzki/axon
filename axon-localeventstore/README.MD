# Local Event Store

This module provides an event sotre which is located within a bounded context. The events stored in this event store are not accessible by other bounded contexts. Nevertheless they can be exposed to the global event store. The global event store can be an Axon Server instance or an Embedded Server instance.

## Why?
Having a Local Event Store has two main benefits
### Local transaction
Persisting the events takes part in a local transaction. It's possible to apply events and to perform other state changes. While this should be avoided for Event-sourced aggregates it's the common usage in the context of State-stored aggregates. This solution addresses the [Consistency gap issue](https://github.com/AxonFramework/AxonFramework/issues/1103).

### Explicit Public API
If you rely on Event-Sourcing you need to apply events for every state changing operation whcih is performed on an aggregate. That's perfectly fine, but you might not want to expose every event to the outside world. Some events might stay in your bounded context and should not be consumed by other bounded contexts.

Instead you might want to publish [summary events](http://verraes.net/2019/05/patterns-for-decoupling-distsys-summary-event/) which are part of your bounded context's public API.

Using the Local Event Store offers the possibility not to push every event to a global (in case of AxonServer, remote) event store. Local events remain in the context that published them.

## How does it work?
The `AxonLocalEventStoreConfiguration` registers a `LocalAndGlobalEventStoreAdapter` as primary `EventStore`.

It configures the `LocalAndGlobalEventStoreAdapter` by passing in a `JpaEventStorageEngine` which acts as the local event store and the `EventStore` with the qualifier *eventStore* which is provided by the *axonserver-connector* or the *embeddedserver-connector*. 

Whenever an event is published the `LocalAndGlobalEventStoreAdapter` stores it in a `JpaEventStorageEngine`. This operation takes part in running transactions and therefore it's possible to perform additional database operations secured by a local transaction.

## How to get events into the global event store?
The `LocalAndGlobalEventStoreAdapter` registers a tracking event processor which notifies the `GlobalEventPublisher` whenever a new event to the local event store.

The `GlobalEventPublisher` publishes all events which are annotated with `@GlobalEvent` to the global event store as-is. It does so by asking the global event store for the aggregate's next sequence number as this number might differ from the local event store if you decide not to publish all the events of an aggregate to the global event store.

For event which is published to the global event store the metadata key *origin* is set to a value which identifies the counded context (by default the *spring.application.name* property).

## What about subscibing / tracking?
Subscribung and tracking event processors register at the `LocalAndGlobalEventStoreAdapter`. The `LocalAndGlobalEventStoreAdapter` uses a `MultiStreamableMessageSource` to work with the local **and** the global event store.

All the events are passed to the event handlers with one exception: Events which have are originated by the bounded context the `LocalAndGlobalEventStoreAdapter` lives in are ignored as they are already processed by tracking the local event store. The metadata key 'origin' is used to identify these events.

From an event listener's point of view it's irrelevant if it tracks a local or a global event.

# What about commands and queries?
Commands and queries are not persisted and therefore they are not handled by this module. Subscribing to commands/queries and publishing them is just performed by the components bound by the Axon Framework.