package de.libutzki.axon.embeddedserverconnector;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.axonframework.common.Registration;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.MessageHandler;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.axonframework.queryhandling.QueryUpdateEmitter;

import de.libutzki.axon.embeddedserver.EmbeddedServer;

/**
 * The {@code EmbeddedServerQueryBus} connects a local {@link QueryBus} with an {@link EmbeddedServer}. This makes sure
 * that queries can be executed across module boundaries. All queries are basically forwarded to the embedded server.
 */
final class EmbeddedServerQueryBus implements QueryBus {

	private final EmbeddedServer embeddedServer;
	private final QueryBus localQueryBus;

	EmbeddedServerQueryBus( final EmbeddedServer embeddedServer, final QueryBus localQueryBus ) {
		this.embeddedServer = embeddedServer;
		this.localQueryBus = localQueryBus;
	}

	@Override
	public Registration registerHandlerInterceptor( final MessageHandlerInterceptor<? super QueryMessage<?, ?>> handlerInterceptor ) {
		return localQueryBus.registerHandlerInterceptor( handlerInterceptor );
	}

	@Override
	public Registration registerDispatchInterceptor( final MessageDispatchInterceptor<? super QueryMessage<?, ?>> dispatchInterceptor ) {
		return localQueryBus.registerDispatchInterceptor( dispatchInterceptor );
	}

	@Override
	public <R> Registration subscribe( final String queryName, final Type responseType, final MessageHandler<? super QueryMessage<?, R>> handler ) {
		final Registration delegateRegistration = localQueryBus.subscribe( queryName, responseType, handler );
		embeddedServer.registerQueryBusForQuery( localQueryBus, queryName );
		return ( ) -> {
			final boolean delegateCancelationSuccessful = delegateRegistration.cancel( );
			final boolean wrapperCancelationSuccessful = embeddedServer.unregisterQueryBusForQuery( localQueryBus, queryName );
			return delegateCancelationSuccessful && wrapperCancelationSuccessful;
		};
	}

	@Override
	public <Q, R> CompletableFuture<QueryResponseMessage<R>> query( final QueryMessage<Q, R> query ) {
		return embeddedServer.query( query );
	}

	@Override
	public <Q, R> Stream<QueryResponseMessage<R>> scatterGather( final QueryMessage<Q, R> query, final long timeout, final TimeUnit unit ) {
		return embeddedServer.scatterGather( query, timeout, unit );
	}

	@Override
	public QueryUpdateEmitter queryUpdateEmitter( ) {
		// We use the query update emitter of the local query bus. This is analogous to how the real axon server connector does
		// it.
		return localQueryBus.queryUpdateEmitter( );
	}

}
