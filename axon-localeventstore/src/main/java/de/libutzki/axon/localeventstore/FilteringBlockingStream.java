package de.libutzki.axon.localeventstore;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import org.axonframework.common.stream.BlockingStream;

import lombok.RequiredArgsConstructor;

/**
 * This class represents an implementation of an Axon {@link BlockingStream} which is able o filter elements based on a
 * given {@link Predicate}.
 *
 * @param <M>
 *            The type of the stream.
 */
@RequiredArgsConstructor
final class FilteringBlockingStream<M> implements BlockingStream<M> {

	private final BlockingStream<M> delegate;
	private final Predicate<M> filter;

	@Override
	public Optional<M> peek( ) {
		if ( !forwardDelegateToNextNotFiltered( ) ) {
			return Optional.empty( );
		}
		return delegate.peek( );
	}

	@Override
	public boolean hasNextAvailable( ) {
		return forwardDelegateToNextNotFiltered( );
	}

	/**
	 * Forwards the delegate until the next element is either not filtered or until the delegate does not contain any more
	 * elements.
	 *
	 * @return true if and only if the delegate contains a next element and this element would not be filtered.
	 */
	private boolean forwardDelegateToNextNotFiltered( ) {
		while ( delegate.hasNextAvailable( ) ) {
			final Optional<M> peeked = delegate.peek( );
			if ( isElementNotFiltered( peeked ) ) {
				return true;
			}

			try {
				skipCurrentElement( );
			} catch ( final InterruptedException e ) {
				Thread.currentThread( ).interrupt( );
			}
		}

		return false;
	}

	private boolean isElementNotFiltered( final Optional<M> peeked ) {
		return peeked.filter( filter ).isPresent( );
	}

	private void skipCurrentElement( ) throws InterruptedException {
		delegate.nextAvailable( );
	}

	@Override
	public boolean hasNextAvailable( final int timeout, final TimeUnit unit ) throws InterruptedException {
		// We need special handling for the zero case, as we might need to skip some more elements.
		if ( timeout == 0 ) {
			return hasNextAvailable( );
		}

		final long deadline = System.currentTimeMillis( ) + unit.toMillis( timeout );
		final long longPollTime = unit.toMillis( timeout ) / 1;
		final IntSupplier timeoutInMillisecondsSupplier = ( ) -> ( int ) Math.min( longPollTime, deadline - System.currentTimeMillis( ) );

		do {
			if ( !delegate.hasNextAvailable( timeoutInMillisecondsSupplier.getAsInt( ), TimeUnit.MILLISECONDS ) ) {
				return false;
			}

			final Optional<M> peeked = delegate.peek( );
			if ( isElementNotFiltered( peeked ) ) {
				return true;
			}

			skipCurrentElement( );
		} while ( timeoutInMillisecondsSupplier.getAsInt( ) > 0 );

		return false;
	}

	@Override
	public M nextAvailable( ) throws InterruptedException {
		while ( !hasNextAvailable( ) ) {
			Thread.sleep( 1 );
		}
		return delegate.nextAvailable( );
	}

	@Override
	public void close( ) {
		delegate.close( );
	}

}
