# Embedded Server Connector
The embeddedserver-connector is to the EmbeddedServer what the axonserver-connector is to the AxonServer.

The connector provides an `EventStore`, a `CommandBus` and a `QueryBus`. Those implementations mostly delegate calls to the `EmbeddedServer`.

## Upcasters
The `EmbeddedServer` interface provides 'raw' event data by returning a stream of `org.axonframework.eventhandling.EventData`. Similar to what the axonserver-connector does the embeddedserver-connector is responsible for applying the upcaster chain and converting the `EventData` stream to `org.axonframework.messaging.Message` stream.

The implementatin details can be found in `EmbeddedServerEventStore` and `EmbeddedServerTrackingEventStream`.