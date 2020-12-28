package de.libutzki.axon.embeddedserver;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.NoHandlerForCommandException;
import org.axonframework.common.Registration;
import org.axonframework.eventhandling.DomainEventData;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.TrackedEventData;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.queryhandling.NoHandlerForQueryException;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;

/**
 * The {@code EmbeddedServer} replaces the real axon server in case that we do not want to distribute the application.
 * In this case all communication is performed within the JVM and via this server.<br>
 * <br>
 * The {@code EmbeddedServer} is threadsafe, as long as the registered {@link CommandBus CommandBusses} and
 * {@link QueryBus QueryBusses} are threadsafe.
 */
public interface EmbeddedServer {

	/**
	 * Dispatches the given command to the responsible command bus.
	 *
	 * @param <C>
	 *                 The type of payload of the command.
	 * @param <R>
	 *                 The type of result of the command handling.
	 * @param command
	 *                 The command to dispatch.
	 * @param callback
	 *                 The corresponding callback.
	 *
	 * @throws NoHandlerForCommandException
	 *                                      If there is currently no command bus registered to handle the command.
	 */
	<C, R> void dispatch( CommandMessage<C> command, CommandCallback<? super C, ? super R> callback );

	/**
	 * Registers the given command bus for the given command name.
	 *
	 * @param commandBus
	 *                    The command bus to be registered
	 * @param commandName
	 *                    The command name.
	 *
	 * @throws IllegalStateException
	 *                               If there is already a command bus registered for the given command name.
	 */
	void registerCommandBusForCommand( CommandBus commandBus, String commandName );

	/**
	 * Unregisters the command bus for the given command name.
	 *
	 * @param commandName
	 *                    The command name.
	 *
	 * @return true if and only if the command bus for the given command name has been removed.
	 */
	boolean unregisterCommandBusForCommand( String commandName );

	/**
	 * Registers the given query bus for the given query name.
	 *
	 * @param queryBus
	 *                  The query bus to be registered
	 * @param queryName
	 *                  The command name.
	 */
	void registerQueryBusForQuery( QueryBus queryBus, String queryName );

	/**
	 * Unregisters the given query bus for the given query name.
	 *
	 * @param queryBus
	 *                  The query bus to be registered
	 * @param queryName
	 *                  The command name.
	 *
	 * @return true if and only if the query bus has been removed.
	 */
	boolean unregisterQueryBusForQuery( QueryBus localQueryBus, String queryName );

	/**
	 * Executes the given query with the responsible query bus.
	 *
	 * @param <Q>
	 *              The type of payload of the query.
	 * @param <R>
	 *              The type of result of the query.
	 * @param query
	 *              The query to execute.
	 *
	 * @return The result of the query execution.
	 *
	 * @throws NoHandlerForQueryException
	 *                                    If there is no or multiple handlers for the given query.
	 */
	<Q, R> CompletableFuture<QueryResponseMessage<R>> query( QueryMessage<Q, R> query );

	/**
	 * Dispatch the given {@code query} to all QueryHandlers subscribed to the given {@code query}'s queryName/responseType.
	 * Returns a stream of results which blocks until all handlers have processed the request or when the timeout occurs.
	 * <p>
	 * If no handlers are available to provide a result, or when all available handlers throw an exception while attempting
	 * to do so, the returned Stream is empty.
	 * <p>
	 * Note that any terminal operation (such as {@link Stream#forEach(Consumer)}) on the Stream may cause it to block until
	 * the {@code timeout} has expired, awaiting additional data to include in the stream.
	 *
	 * @param query
	 *                the query
	 * @param timeout
	 *                time to wait for results
	 * @param unit
	 *                unit for the timeout
	 * @param <Q>
	 *                the payload type of the query
	 * @param <R>
	 *                the response type of the query
	 * @return stream of query results
	 */
	<Q, R> Stream<QueryResponseMessage<R>> scatterGather( QueryMessage<Q, R> query, long timeout, TimeUnit unit );

	/**
	 * Registers the given event bus for the given event name.
	 *
	 * @param eventBus
	 *                  The event bus to be registered
	 * @param eventName
	 *                  The event name.
	 */
	Registration registerEventProcessor( Consumer<List<? extends EventMessage<?>>> messageProcessor );

	/**
	 * Publish a collection of events on this bus (one, or multiple). The events will be dispatched to all subscribed
	 * listeners.
	 * <p>
	 * Implementations may treat the given {@code events} as a single batch and distribute the events as such to all
	 * subscribed EventListeners.
	 *
	 * @param events
	 *               The collection of events to publish
	 */
	void publish( List<? extends EventMessage<?>> events );

	/**
	 * Open a stream containing all {@link TrackedEventData} since given tracking token. Pass a {@code trackingToken} of
	 * {@code null} to open a stream containing all available events. Note that the returned stream is <em>infinite</em>, so
	 * beware of applying terminal operations to the returned stream.
	 *
	 * @param trackingToken
	 *                      object containing the position in the stream or {@code null} to open a stream containing all
	 *                      messages
	 * @return a stream of events since the given trackingToken
	 */
	Stream<? extends TrackedEventData<?>> openStream( TrackingToken trackingToken );

	/**
	 * Returns a stream of serialized event entries for given {@code aggregateIdentifier}.
	 * <p>
	 * The returned stream is <em>finite</em>, ending with the last known event of the aggregate. If the event store holds
	 * no events of the given aggregate an empty stream is returned.
	 *
	 * @param aggregateIdentifier
	 *                            the identifier of the aggregate whose event entries to fetch
	 * @return a stream of all currently stored event entries of the aggregate
	 */
	Stream<? extends DomainEventData<?>> readEvents( String aggregateIdentifier );

	/**
	 * Returns a stream of serialized event entries for given {@code aggregateIdentifier}.
	 * <p>
	 * The returned stream is <em>finite</em>, ending with the last known event of the aggregate. If the event store holds
	 * no events of the given aggregate an empty stream is returned.
	 *
	 * @param aggregateIdentifier
	 *                            the identifier of the aggregate whose event entries to fetch
	 * @return a stream of all currently stored event entries of the aggregate
	 */
	Stream<? extends DomainEventData<?>> readEvents( String aggregateIdentifier, final long firstSequenceNumber );

	/**
	 * Returns a stream of serialized event entries for given {@code aggregateIdentifier} if the backing database contains a
	 * snapshot of the aggregate.
	 * <p>
	 * It is required that specific event storage engines return snapshots in descending order of their sequence number.
	 * </p>
	 *
	 * @param aggregateIdentifier
	 *                            The aggregate identifier to fetch a snapshot for
	 * @return A stream of serialized snapshots of the aggregate
	 */
	Stream<? extends DomainEventData<?>> readSnapshotData( String aggregateIdentifier );

	/**
	 * Stores the given (temporary) {@code snapshot} event. This snapshot replaces the segment of the event stream
	 * identified by the {@code snapshot}'s {@link DomainEventMessage#getAggregateIdentifier() Aggregate Identifier} up to
	 * (and including) the event with the {@code snapshot}'s {@link DomainEventMessage#getSequenceNumber() sequence number}.
	 * <p>
	 * These snapshots will only affect the {@link DomainEventStream} returned by the {@link #readEvents(String)} method.
	 * They do not change the events returned by {@link EventStore#openStream(TrackingToken)} or those received by using
	 * {@link #subscribe(java.util.function.Consumer)}.
	 * <p>
	 * Note that snapshots are considered a temporary replacement for Events, and are used as performance optimization.
	 * Event Store implementations may choose to ignore or delete snapshots.
	 *
	 * @param snapshot
	 *                 The snapshot to replace part of the DomainEventStream.
	 */
	void storeSnapshot( DomainEventMessage<?> snapshot );

	/**
	 * Register the given {@code interceptor} with this bus. When subscribed it will intercept any event messages published
	 * on this bus.
	 * <p>
	 * If the given {@code interceptor} is already registered, nothing happens.
	 *
	 * @param dispatchInterceptor
	 *                            The event message dispatch interceptor to register
	 * @return a handle to unregister the {@code dispatchInterceptor}. When unregistered it will no longer be given event
	 *         messages published on this bus.
	 */
	Registration registerDispatchInterceptor( MessageDispatchInterceptor<? super EventMessage<?>> dispatchInterceptor );

}