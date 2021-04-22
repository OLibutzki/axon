package de.libutzki.axon.localeventstore;

import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configurer;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.messaging.annotation.ParameterResolverFactory;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.config.AxonConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty( name = "axon.localeventstore.enabled", matchIfMissing = true )

@AutoConfigureBefore( name = { "org.axonframework.springboot.autoconfig.AxonAutoConfiguration", "org.axonframework.springboot.autoconfig.JpaAutoConfiguration" } )
@AutoConfigureAfter( name = "org.axonframework.springboot.autoconfig.AxonServerAutoConfiguration" )
@EntityScan( { "org.axonframework.eventhandling.tokenstore", "org.axonframework.modelling.saga.repository.jpa", "org.axonframework.eventsourcing.eventstore.jpa" } )
public class AxonLocalEventStoreConfiguration {

	@Autowired
	void configureEventProcessing( final Configurer configurer, @Value( "${spring.application.name}" ) final String applicationName ) {
		configurer.eventProcessing( ).registerDefaultHandlerInterceptor( ( c, p ) -> new OriginFilteringEventHandlerInterceptor( applicationName ) );
	}

	@Bean
	@Primary
	EventStore localAndGlobalEventStoreAdapter( final EventStorageEngine storageEngine, @Qualifier( "eventStore" ) final EventStore globalEventStore, final org.axonframework.config.Configuration configuration, final ParameterResolverFactory parameterResolveFactory, @Value( "${spring.application.name}" ) final String applicationName ) {
		final EmbeddedEventStore localEventStore = EmbeddedEventStore.builder( )
				.storageEngine( storageEngine )
				.messageMonitor( configuration.messageMonitor( EventStore.class, "localEventStore" ) )
				.build( );
		configuration.onShutdown( localEventStore::shutDown );
		return new LocalAndGlobalEventStoreAdapter( localEventStore, globalEventStore, configuration, parameterResolveFactory, applicationName );
	}

	@Bean
	EventStorageEngine eventStorageEngine( final Serializer defaultSerializer,
			final PersistenceExceptionResolver persistenceExceptionResolver,
			@Qualifier( "eventSerializer" ) final Serializer eventSerializer,
			final AxonConfiguration configuration,
			final EntityManagerProvider entityManagerProvider,
			final TransactionManager transactionManager ) {
		return JpaEventStorageEngine.builder( )
				.snapshotSerializer( defaultSerializer )
				.upcasterChain( configuration.upcasterChain( ) )
				.persistenceExceptionResolver( persistenceExceptionResolver )
				.eventSerializer( eventSerializer )
				.snapshotFilter( configuration.snapshotFilter( ) )
				.entityManagerProvider( entityManagerProvider )
				.transactionManager( transactionManager )
				.build( );
	}
}