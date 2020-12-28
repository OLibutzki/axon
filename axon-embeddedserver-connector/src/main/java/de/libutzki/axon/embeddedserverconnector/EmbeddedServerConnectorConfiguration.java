package de.libutzki.axon.embeddedserverconnector;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.queryhandling.LoggingQueryInvocationErrorHandler;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryInvocationErrorHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.queryhandling.SimpleQueryBus;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.config.AxonConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.libutzki.axon.embeddedserver.EmbeddedServer;

/**
 * This configuration activates the connectors for the embedded server case if the {@link EmbeddedServer} bean is
 * available.
 */
@Configuration
@AutoConfigureBefore( name = "org.axonframework.springboot.autoconfig.AxonServerAutoConfiguration" )
@ConditionalOnBean( EmbeddedServer.class )
@ConditionalOnProperty( name = "axon.embeddedserver.enabled", matchIfMissing = true )
public class EmbeddedServerConnectorConfiguration {

	@Bean( name = "eventStore" )
	@Qualifier( "eventBus" )
	@ConditionalOnMissingBean( name = "eventStore" )
	public EventStore embeddedEventStore( final EmbeddedServer embeddedServer, @Qualifier( "eventSerializer" ) final Serializer eventSerializer,
			final AxonConfiguration configuration ) {
		return new EmbeddedServerEventStore( embeddedServer, eventSerializer, configuration.upcasterChain( ) );
	}

	@Bean
	@Qualifier( "localSegment" )
	@ConditionalOnMissingBean( name = "commandBus" )
	public CommandBus commandBus( final EmbeddedServer embeddedServer, final AxonConfiguration axonConfiguration, final TransactionManager transactionManager ) {
		final SimpleCommandBus simpleCommandBus = SimpleCommandBus.builder( )
				.transactionManager( transactionManager )
				.messageMonitor( axonConfiguration.messageMonitor( CommandBus.class, "commandBus" ) )
				.build( );
		simpleCommandBus.registerHandlerInterceptor( new CorrelationDataInterceptor<>( axonConfiguration.correlationDataProviders( ) ) );

		return new EmbeddedServerCommandBus( embeddedServer, simpleCommandBus );
	}

	@Bean
	@Qualifier( "localSegment" )
	@ConditionalOnMissingBean( name = "queryBus" )
	public QueryBus queryBus( final EmbeddedServer embeddedServer, final AxonConfiguration axonConfiguration, final TransactionManager transactionManager ) {
		final SimpleQueryBus simpleQueryBus = SimpleQueryBus.builder( )
				.messageMonitor( axonConfiguration.messageMonitor( QueryBus.class, "queryBus" ) )
				.transactionManager( transactionManager )
				.queryUpdateEmitter( axonConfiguration.getComponent( QueryUpdateEmitter.class ) )
				.errorHandler( axonConfiguration.getComponent(
						QueryInvocationErrorHandler.class,
						( ) -> LoggingQueryInvocationErrorHandler.builder( ).build( ) ) )
				.build( );
		simpleQueryBus.registerHandlerInterceptor( new CorrelationDataInterceptor<>( axonConfiguration.correlationDataProviders( ) ) );

		return new EmbeddedServerQueryBus( embeddedServer, simpleQueryBus );
	}

}
