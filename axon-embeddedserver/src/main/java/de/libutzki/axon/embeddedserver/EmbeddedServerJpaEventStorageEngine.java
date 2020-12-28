package de.libutzki.axon.embeddedserver;

import java.util.Optional;
import java.util.stream.Stream;

import org.axonframework.eventhandling.DomainEventData;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.TrackedEventData;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;

/**
 * In contrast to the {@link JpaEventStorageEngine} the {@link EmbeddedServerJpaEventStorageEngine} provides a public
 * {@link #readEventData(TrackingToken)} method in order to provide a stream of {@link TrackedEventData}. This stream
 * can be used by clients in order to apply an upcaster chain.
 *
 * @author oliver.libutzki
 *
 */
final class EmbeddedServerJpaEventStorageEngine extends JpaEventStorageEngine {

	public static Builder builder( ) {
		return new Builder( );
	}

	public static class Builder extends org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine.Builder {
		@Override
		public EmbeddedServerJpaEventStorageEngine build( ) {
			return new EmbeddedServerJpaEventStorageEngine( this );
		}
	}

	protected EmbeddedServerJpaEventStorageEngine( final Builder builder ) {
		super( builder );
	}

	@Override
	public Stream<? extends TrackedEventMessage<?>> readEvents( final TrackingToken trackingToken, final boolean mayBlock ) {
		throw createUnsupportedOperationException( );
	}

	@Override
	public DomainEventStream readEvents( final String aggregateIdentifier, final long firstSequenceNumber ) {
		throw createUnsupportedOperationException( );
	}

	@Override
	public Optional<DomainEventMessage<?>> readSnapshot( final String aggregateIdentifier ) {
		throw createUnsupportedOperationException( );
	}

	@Override
	public Stream<? extends TrackedEventData<?>> readEventData( final TrackingToken trackingToken, final boolean mayBlock ) {
		return super.readEventData( trackingToken, mayBlock );
	}

	@Override
	public Stream<? extends DomainEventData<?>> readEventData( final String identifier, final long firstSequenceNumber ) {
		return super.readEventData( identifier, firstSequenceNumber );
	}

	@Override
	public Stream<? extends DomainEventData<?>> readSnapshotData( final String aggregateIdentifier ) {
		return super.readSnapshotData( aggregateIdentifier );
	}

	private UnsupportedOperationException createUnsupportedOperationException( ) {
		return new UnsupportedOperationException( "In an embedded server scenario the server never returns messages. Clients are responsible for creating messages." );
	}

}
