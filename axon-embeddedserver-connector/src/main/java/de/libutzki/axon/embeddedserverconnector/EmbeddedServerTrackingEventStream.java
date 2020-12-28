package de.libutzki.axon.embeddedserverconnector;

import static org.axonframework.common.ObjectUtils.getOrDefault;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.axonframework.eventhandling.EventUtils;
import org.axonframework.eventhandling.TrackedEventData;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingEventStream;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;
import org.axonframework.serialization.upcasting.event.NoOpEventUpcaster;

import lombok.extern.log4j.Log4j2;

/**
 * The {@link EmbeddedServerTrackingEventStream} is responsible for transforming a stream of {@link TrackedEventData}
 * into a stream of {@link TrackedEventMessage}. It does so by applying
 * {@link EventUtils#upcastAndDeserializeTrackedEvents(Stream, Serializer, EventUpcaster)}.
 *
 * @author oliver.libutzki
 *
 */
@Log4j2
final class EmbeddedServerTrackingEventStream implements TrackingEventStream {

	private final Stream<TrackedEventMessage<?>> eventStream;
	private final Iterator<TrackedEventMessage<?>> eventStreamIterator;
	private TrackedEventMessage<?> peekEvent;

	public EmbeddedServerTrackingEventStream( final Stream<? extends TrackedEventData<?>> trackedEventDataStream, final EventUpcaster upcasterChain, final Serializer serializer ) {
		this.eventStream = EventUtils.upcastAndDeserializeTrackedEvents(
				trackedEventDataStream,
				serializer,
				getOrDefault( upcasterChain, NoOpEventUpcaster.INSTANCE ) );
		this.eventStreamIterator = eventStream.iterator( );

	}

	@Override
	public Optional<TrackedEventMessage<?>> peek( ) {
		if ( peekEvent == null && eventStreamIterator.hasNext( ) ) {
			peekEvent = eventStreamIterator.next( );
		}
		return Optional.ofNullable( peekEvent );
	}

	@Override
	public boolean hasNextAvailable( final int timeout, final TimeUnit timeUnit ) throws InterruptedException {
		final long deadline = System.currentTimeMillis( ) + timeUnit.toMillis( timeout );
		try {
			while ( peekEvent == null && !eventStreamIterator.hasNext( ) && System.currentTimeMillis( ) < deadline ) {
				Thread.sleep( 100 );
			}
			return peekEvent != null || eventStreamIterator.hasNext( );
		} catch ( final InterruptedException e ) {
			log.warn( "Consumer thread was interrupted. Returning thread to event processor.", e );
			Thread.currentThread( ).interrupt( );
			return false;
		}
	}

	@Override
	public TrackedEventMessage<?> nextAvailable( ) {
		try {
			hasNextAvailable( Integer.MAX_VALUE, TimeUnit.MILLISECONDS );
			return peekEvent == null ? eventStreamIterator.next( ) : peekEvent;
		} catch ( final InterruptedException e ) {
			log.warn( "Consumer thread was interrupted. Returning thread to event processor.", e );
			Thread.currentThread( ).interrupt( );
			return null;
		} finally {
			peekEvent = null;
		}
	}

	@Override
	public void close( ) {
		eventStream.close( );
	}
}
