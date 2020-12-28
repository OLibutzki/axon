package de.libutzki.axon.localeventstore;

import java.util.Collections;
import java.util.Optional;

import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.DomainEventSequenceAware;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventMessageHandler;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * The {@link GlobalEventPublisher} is responsible for copying non-local events (events not marked with
 * {@link GlobalEvent}) into the global event store. It also marks the events with their origin module.
 */
@Log4j2
@RequiredArgsConstructor
final class GlobalEventPublisher implements EventMessageHandler {

	private final EventStore globalEventStore;
	private final String origin;

	@Override
	public Object handle( final EventMessage<?> eventMessage ) throws Exception {
		final Class<?> payloadType = eventMessage.getPayloadType( );

		if ( payloadType.isAnnotationPresent( GlobalEvent.class ) ) {
			final EventMessage<?> eventMessageToBePublished;
			if ( eventMessage instanceof DomainEventMessage && globalEventStore instanceof DomainEventSequenceAware ) {
				final DomainEventMessage<?> domainEventMessage = ( DomainEventMessage<?> ) eventMessage;
				final String aggregateIdentifier = domainEventMessage.getAggregateIdentifier( );
				final Optional<Long> sequenceNumber = globalEventStore.lastSequenceNumberFor( aggregateIdentifier );
				if ( domainEventMessage.getType( ) == null ) {
					eventMessageToBePublished = new GenericEventMessage<>( domainEventMessage, domainEventMessage::getTimestamp );
				} else {
					eventMessageToBePublished = new GenericDomainEventMessage<>( domainEventMessage.getType( ), domainEventMessage.getAggregateIdentifier( ), sequenceNumber.map( seq -> seq + 1 ).orElse( 0L ), domainEventMessage, domainEventMessage.getTimestamp( ) );
				}
			} else {
				eventMessageToBePublished = eventMessage;
			}
			log.debug( ( ) -> "Event published to global event store: " + eventMessage );
			globalEventStore.publish( eventMessageToBePublished.andMetaData( Collections.singletonMap( MetadataKeys.ORIGIN, origin ) ) );
		}

		return null;
	}
}
