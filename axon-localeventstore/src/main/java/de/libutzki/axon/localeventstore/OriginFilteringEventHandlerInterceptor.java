package de.libutzki.axon.localeventstore;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;

/**
 * This interceptor makes sure that events with the own origin are filtered. This ensures that events originating from
 * the same bounded context are not processed twice.
 */
final class OriginFilteringEventHandlerInterceptor implements MessageHandlerInterceptor<EventMessage<?>> {

	private final String origin;

	OriginFilteringEventHandlerInterceptor( final String origin ) {
		this.origin = origin;
	}

	@Override
	public Object handle( final UnitOfWork<? extends EventMessage<?>> unitOfWork, final InterceptorChain interceptorChain ) throws Exception {
		final EventMessage<?> eventMessage = unitOfWork.getMessage( );
		final boolean eventIsFromOwnOrigin = eventMessage.getMetaData( ).getOrDefault( MetadataKeys.ORIGIN, "" ).equals( origin );

		if ( eventIsFromOwnOrigin ) {
			return null;
		}

		return interceptorChain.proceed( );
	}

}