package de.libutzki.axon.localeventstore;

import java.util.List;
import java.util.function.Consumer;
import org.axonframework.common.Registration;
import org.axonframework.common.stream.BlockingStream;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.SimpleEventHandlerInvoker;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventhandling.async.SequentialPolicy;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventsourcing.MultiStreamableMessageSource;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.messaging.annotation.ParameterResolverFactory;

/**
 * The {@link LocalAndGlobalEventStoreAdapter} is an adapter between the local and the global event store. Registrations
 * are dispatched and events are concatenated.
 */
public final class LocalAndGlobalEventStoreAdapter implements EventStore {

	private final EventStore localEventStore;
	private final EventStore globalEventStore;

	private final StreamableMessageSource<TrackedEventMessage<?>> messageSource;
	private final TrackingEventProcessor trackingEventProcessor;

	public LocalAndGlobalEventStoreAdapter( final EventStore localEventStore, final EventStore globalEventStore, final Configuration configuration, final ParameterResolverFactory parameterResolverFactory, final String origin ) {
		this.localEventStore = localEventStore;
		this.globalEventStore = globalEventStore;
		messageSource = MultiStreamableMessageSource.builder( )
				.addMessageSource( "globalEventStore", globalEventStore )
				.addMessageSource( "localEventStore", localEventStore )
				.build( );

		final SimpleEventHandlerInvoker eventHandlerInvoker = SimpleEventHandlerInvoker.builder( )
				.sequencingPolicy( new SequentialPolicy( ) )
				.parameterResolverFactory( parameterResolverFactory )
				.eventHandlers( new GlobalEventPublisher( globalEventStore, origin ) )
				.build( );

		trackingEventProcessor = TrackingEventProcessor.builder( )
				.name( "localEventStoreTracker" )
				.eventHandlerInvoker( eventHandlerInvoker )
				.trackingEventProcessorConfiguration( TrackingEventProcessorConfiguration.forSingleThreadedProcessing( ) )
				.messageMonitor( configuration.messageMonitor( TrackingEventProcessor.class, "localEventStoreTracker" ) )
				.messageSource( localEventStore )
				.tokenStore( configuration.getComponent( TokenStore.class ) )
				.transactionManager( configuration.getComponent( TransactionManager.class ) )
				.build( );
		trackingEventProcessor.start( );
	}

	public void shutdown( ) {
		trackingEventProcessor.shutDown( );
	}

	@Override
	public void publish( final List<? extends EventMessage<?>> events ) {
		localEventStore.publish( events );
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
