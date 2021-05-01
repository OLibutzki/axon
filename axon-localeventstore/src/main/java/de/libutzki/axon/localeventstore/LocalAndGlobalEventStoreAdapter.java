package de.libutzki.axon.localeventstore;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.axonframework.common.Registration;
import org.axonframework.common.stream.BlockingStream;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.MultiStreamableMessageSource;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.StreamableMessageSource;

/**
 * The {@link LocalAndGlobalEventStoreAdapter} is an adapter between the local and the global event store. Registrations
 * are dispatched and events are concatenated.
 */
public final class LocalAndGlobalEventStoreAdapter implements EventStore {

	private final EventStore localEventStore;
	private final EventStore globalEventStore;

	private final StreamableMessageSource<TrackedEventMessage<?>> messageSource;

	public LocalAndGlobalEventStoreAdapter( final EventStore localEventStore, final EventStore globalEventStore ) {
		this.localEventStore = localEventStore;
		this.globalEventStore = globalEventStore;
		messageSource = MultiStreamableMessageSource.builder( )
				.addMessageSource( "globalEventStore", globalEventStore )
				.addMessageSource( "localEventStore", localEventStore )
				.build( );
	}

	@Override
	public void publish( final List<? extends EventMessage<?>> events ) {
		Map<Boolean, List<EventMessage<?>>> partitions = events.stream().collect(Collectors.partitioningBy(eventMessage -> eventMessage.getPayloadType( ).isAnnotationPresent(GlobalEvent.class)));
		List<EventMessage<?>> localEvents = partitions.getOrDefault(Boolean.FALSE, Collections.emptyList());
		if (!localEvents.isEmpty()) {
			localEventStore.publish(localEvents);
		}
		List<EventMessage<?>> globalEvents = partitions.getOrDefault(Boolean.TRUE, Collections.emptyList());
		if (!globalEvents.isEmpty()) {
			globalEventStore.publish(globalEvents);
		}
	}

	@Override
	public Registration registerDispatchInterceptor( final MessageDispatchInterceptor<? super EventMessage<?>> dispatchInterceptor ) {
		final Registration localEventStoreRegistration = localEventStore.registerDispatchInterceptor( dispatchInterceptor );
		final Registration globalEventStoreRegistration = globalEventStore.registerDispatchInterceptor( dispatchInterceptor );
		return ( ) -> {
			final boolean localEventStoreCancelationSuccessful = localEventStoreRegistration.cancel( );
			final boolean globalEventStoreCancelationSuccessful = globalEventStoreRegistration.cancel( );
			return localEventStoreCancelationSuccessful && globalEventStoreCancelationSuccessful;
		};
	}

	@Override
	public Registration subscribe( final Consumer<List<? extends EventMessage<?>>> messageProcessor ) {
		final Registration localEventStoreRegistration = localEventStore.subscribe( messageProcessor );
		final Registration globalEventStoreRegistration = globalEventStore.subscribe( messageProcessor );
		return ( ) -> {
			final boolean localEventStoreCancelationSuccessful = localEventStoreRegistration.cancel( );
			final boolean globalEventStoreCancelationSuccessful = globalEventStoreRegistration.cancel( );
			return localEventStoreCancelationSuccessful && globalEventStoreCancelationSuccessful;
		};
	}

	@Override
	public BlockingStream<TrackedEventMessage<?>> openStream( final TrackingToken trackingToken ) {
		return messageSource.openStream( trackingToken );
	}

	@Override
	public DomainEventStream readEvents( final String aggregateIdentifier ) {
		final DomainEventStream localEventStoreEventStream = localEventStore.readEvents( aggregateIdentifier );
		final DomainEventStream globalEventStoreEventStream = globalEventStore.readEvents( aggregateIdentifier );
		return DomainEventStream.concat( localEventStoreEventStream, globalEventStoreEventStream );
	}

	@Override
	public void storeSnapshot( final DomainEventMessage<?> snapshot ) {
		localEventStore.storeSnapshot( snapshot );
		globalEventStore.storeSnapshot( snapshot );
	}
}
