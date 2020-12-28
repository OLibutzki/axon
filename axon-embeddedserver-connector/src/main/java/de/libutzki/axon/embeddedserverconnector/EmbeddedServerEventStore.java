package de.libutzki.axon.embeddedserverconnector;

import static org.axonframework.eventsourcing.EventStreamUtils.upcastAndDeserializeDomainEvents;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.axonframework.common.Registration;
import org.axonframework.common.stream.BlockingStream;
import org.axonframework.eventhandling.DomainEventData;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.TrackedEventData;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;

import de.libutzki.axon.embeddedserver.EmbeddedServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * The {@link EmbeddedServerEventStore} wraps an {@link EmbeddedServer} to avoid that the server has to implement the
 * {@link EventStore} interface. It basically dispatches each method to the embedded server. Furthermore, it applies the
 * upcaster chain when calling {@link #openStream(TrackingToken)} or {@link #readEvents(String)}.
 */
@RequiredArgsConstructor
@Log4j2
final class EmbeddedServerEventStore implements EventStore {

	private final EmbeddedServer embeddedServer;
	private final Serializer serializer;
	private final EventUpcaster upcasterChain;

	@Override
	public Registration subscribe( final Consumer<List<? extends EventMessage<?>>> eventProcessor ) {
		return embeddedServer.registerEventProcessor( eventProcessor );
	}

	@Override
	public void publish( final List<? extends EventMessage<?>> events ) {
		embeddedServer.publish( events );

	}

	@Override
	public Registration registerDispatchInterceptor( final MessageDispatchInterceptor<? super EventMessage<?>> dispatchInterceptor ) {
		return embeddedServer.registerDispatchInterceptor( dispatchInterceptor );
	}

	@Override
	public BlockingStream<TrackedEventMessage<?>> openStream( final TrackingToken trackingToken ) {
		final Stream<? extends TrackedEventData<?>> trackedEventDataStream = embeddedServer.openStream( trackingToken );
		return new EmbeddedServerTrackingEventStream( trackedEventDataStream, upcasterChain, serializer );
	}

	@Override
	public DomainEventStream readEvents( final String aggregateIdentifier ) {
		Optional<? extends DomainEventMessage<?>> optionalSnapshot;
		try {
			optionalSnapshot = readSnapshot( aggregateIdentifier );
		} catch ( Exception | LinkageError e ) {
			log.warn( "Error reading snapshot for aggregate [{}]. Reconstructing from entire event stream.",
					aggregateIdentifier, e );
			optionalSnapshot = Optional.empty( );
		}
		final DomainEventStream eventStream;
		if ( optionalSnapshot.isPresent( ) ) {
			final DomainEventMessage<?> snapshot = optionalSnapshot.get( );

			final Stream<? extends DomainEventData<?>> eventDataStreamAfterSnapshot = embeddedServer.readEvents( aggregateIdentifier, snapshot.getSequenceNumber( ) + 1 );
			final DomainEventStream domainEvents = upcastAndDeserializeDomainEvents( eventDataStreamAfterSnapshot, serializer, upcasterChain );
			eventStream = DomainEventStream.concat( DomainEventStream.of( snapshot ), domainEvents );
		} else {
			final Stream<? extends DomainEventData<?>> eventDataStream = embeddedServer.readEvents( aggregateIdentifier );
			eventStream = upcastAndDeserializeDomainEvents( eventDataStream, serializer, upcasterChain );
		}
		return eventStream;
	}

	private Optional<? extends DomainEventMessage<?>> readSnapshot( final String aggregateIdentifier ) {
		return upcastAndDeserializeDomainEvents( embeddedServer.readSnapshotData( aggregateIdentifier ), serializer, upcasterChain )
				.asStream( )
				.findFirst( );
	}

	@Override
	public void storeSnapshot( final DomainEventMessage<?> snapshot ) {
		embeddedServer.storeSnapshot( snapshot );
	}

}
