# Example
The example project is used to demonstrate how the embedded server and the local event store work in practice.

## Structure
The project uses Spring's hierarchicial application contexts. The configuration is slightly different in the AxonServer and EmbeddedServer scenario.

### Axon Server
By using an AxonServer we have a Parent Context which doe not provide any beans. It just acts as a container for the child contexts and is not needed to operate this setup.

The child contexts (*Child1* and *Child2*) connect to the Axon Server via axonserver-connector. This is just Axon's default behaviour.

```
Parent (no beans in this context)
|
|--- Child1 (uses axonserver-connector)
|
|--- Child2 (uses axonserver-connector)
```

 `AxonServerWithoutLocalEventStoreTest` provides some integration tests to demonstrate this setup. As already mentioned: This is the default behaviour in conjunction with Axon Server. The additional modules provided in this draft are not in play. I just added these tests for the sake of completeness.

 The Axon Server is started using [Testcontainers](https://www.testcontainers.org/), so you need a running Docker Daemon in order to execute the test.

### Embedded Server
Using the *EmbeddedServer* makes the usage of a Parent Context mandatory. The *EmbeddedServer* is one of the Parent Context's children. The `EmbeddedServer` bean is exported to the parent context and the child contexts (*Child1* and *Child2*) can use it to exchange commands, queries and events.

```
Parent (EmbeddedServer)
|
|--- EmbeddedServer (exports the EmbeddedServer bean to the Parent)
|
|--- Child1 (uses embeddedserver-connector)
|
|--- Child2 (uses embeddedserver-connector)
```
`EmbeddedServerWithoutLocalEventStoreTest` provides some integration tests to demonstrate this setup. As the EmbeddedServer is started in the same JVM there is no need for using Testcontainers and spinning up an Axon Server. The EmbeddedServer acts as a drop-in replacement.

## The Local Event Store
Using a Local Event Store does not change the structure explained above. The only difference is that the events are not directly published to the AxonServer or the EmbeddedServer. They are first stored in the LocalEventStore and all the events annoatted with `@GlobalEvent` are published to the global event store.

The corresponding tests are `AxonServerWithLocalEventStoreTest` and `EmbeddedServerWithLocalEventStoreTest`.

