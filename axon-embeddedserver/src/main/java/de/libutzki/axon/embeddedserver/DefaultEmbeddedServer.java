package de.libutzki.axon.embeddedserver;

import static org.axonframework.commandhandling.GenericCommandResultMessage.asCommandResultMessage;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.NoHandlerForCommandException;
import org.axonframework.common.ObjectUtils;
import org.axonframework.common.Registration;
import org.axonframework.eventhandling.DomainEventData;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.TrackedEventData;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.queryhandling.NoHandlerForQueryException;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;

import lombok.RequiredArgsConstructor;

/**
 * The {@code DefaultEmbeddedServer} replaces the real axon server in case that we do not want to distribute the
 * application. In this case all communication is performed within the JVM and via this server.<br>
 * <br>
 * The {@code DefaultEmbeddedServer} is threadsafe, as long as the registered {@link CommandBus CommandBusses} and
 * {@link QueryBus QueryBusses} are threadsafe.
 */
@RequiredArgsConstructor
final class DefaultEmbeddedServer implements EmbeddedServer {

	private final ConcurrentMap<String, CommandBus> commandBusMap = new ConcurrentHashMap<>( );
	private final ConcurrentMap<String, Set<QueryBus>> queryBusMap = new ConcurrentHashMap<>( );

	private final EmbeddedServerJpaEventStorageEngine embeddedServerJpaEventStorageEngine;
	private final EventStore eventStore;

	@Override
	public <C, R> void dispatch( final CommandMessage<C> command, final CommandCallback<? super C, ? super R> callback ) {
		final CommandBus commandBus = commandBusMap.get( command.getCommandName( ) );

		if ( commandBus != null ) {
			commandBus.dispatch( command, callback );
		} else {
			final NoHandlerForCommandException noHandlerForCommandException = new NoHandlerForCommandException( command );
			callback.onResult( command, asCommandResultMessage( noHandlerForCommandException ) );
		}
	}

	@Override
	public void registerCommandBusForCommand( final CommandBus commandBus, final String commandName ) {
		final CommandBus previousCommandBus = commandBusMap.putIfAbsent( commandName, commandBus );
		if ( previousCommandBus != null && previousCommandBus != commandBus ) {
			throw new IllegalStateException( String.format( "Multiple command busses registered for command %s.", commandName ) );
		}
	}

	@Override
	public boolean unregisterCommandBusForCommand( final String commandName ) {
		return commandBusMap.remove( commandName ) != null;
	}

	@Override
	public void registerQueryBusForQuery( final QueryBus queryBus, final String queryName ) {
		final Set<QueryBus> set = queryBusMap.computeIfAbsent( queryName, k -> new CopyOnWriteArraySet<>( ) );
		set.add( queryBus );
	}

	@Override
	public boolean unregisterQueryBusForQuery( final QueryBus localQueryBus, final String queryName ) {
		final Set<QueryBus> set = queryBusMap.getOrDefault( queryName, Collections.emptySet( ) );
		return set.remove( localQueryBus );
	}

	@Override
	public <Q, R> CompletableFuture<QueryResponseMessage<R>> query( final QueryMessage<Q, R> query ) {
		final Set<QueryBus> set = queryBusMap.getOrDefault( query.getQueryName( ), Collections.emptySet( ) );
		if ( set.isEmpty( ) ) {
			throw new NoHandlerForQueryException( String.format( "No handler found for [%s].", query.getQueryName( ) ) );
		}
		if ( set.size( ) > 1 ) {
			throw new NoHandlerForQueryException( String.format( "Multiple handlers found for [%s].", query.getQueryName( ) ) );
		}

		final QueryBus queryBus = set.iterator( ).next( );
		return queryBus.query( query );
	}

	@Override
	public <Q, R> Stream<QueryResponseMessage<R>> scatterGather( final QueryMessage<Q, R> query, final long timeout, final TimeUnit unit ) {
		final Set<QueryBus> set = queryBusMap.getOrDefault( query.getQueryName( ), Collections.emptySet( ) );

		final long deadline = System.currentTimeMillis( ) + unit.toMillis( timeout );
		return set
				.stream( )
				.flatMap( queryBus -> {
					final long leftTimeout = ObjectUtils.getRemainingOfDeadline( deadline );
					return queryBus.scatterGather( query, leftTimeout, TimeUnit.MILLISECONDS );
				} );
	}

	@Override
	public void publish( final List<? extends EventMessage<?>> events ) {
		eventStore.publish( events );

	}

	@Override
	public Stream<? extends TrackedEventData<?>> openStream( final TrackingToken trackingToken ) {
		return embeddedServerJpaEventStorageEngine.readEventData( trackingToken, false );
	}

	@Override
	public Stream<? extends DomainEventData<?>> readEvents( final String aggregateIdentifier ) {
		return embeddedServerJpaEventStorageEngine.readEventData( aggregateIdentifier, 0L );
	}

	@Override
	public Stream<? extends DomainEventData<?>> readEvents( final String aggregateIdentifier, final long firstSequenceNumber ) {
		return embeddedServerJpaEventStorageEngine.readEventData( aggregateIdentifier, firstSequenceNumber );
	}

	@Override
	public Stream<? extends DomainEventData<?>> readSnapshotData( final String aggregateIdentifier ) {
		return embeddedServerJpaEventStorageEngine.readSnapshotData( aggregateIdentifier );
	}

	@Override
	public void storeSnapshot( final DomainEventMessage<?> snapshot ) {
		embeddedServerJpaEventStorageEngine.storeSnapshot( snapshot );
	}

	@Override
	public Registration registerEventProcessor( final Consumer<List<? extends EventMessage<?>>> messageProcessor ) {
		return eventStore.subscribe( messageProcessor );
	}

	@Override
	public Registration registerDispatchInterceptor( final MessageDispatchInterceptor<? super EventMessage<?>> dispatchInterceptor ) {
		return eventStore.registerDispatchInterceptor( dispatchInterceptor );
	}

}
