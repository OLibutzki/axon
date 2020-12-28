package de.libutzki.axon.embeddedserver;

import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.config.AxonConfiguration;
import org.axonframework.springboot.autoconfig.JpaEventStoreAutoConfiguration;
import org.axonframework.springboot.util.RegisterDefaultEntities;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link EmbeddedServerJpaEventStoreConfiguration} provides the {@link EmbeddedServerJpaEventStorageEngine} as
 * {@link EventStorageEngine}. The bean definiton has to be outsourced to a seperate configuration in order to ensure
 * that it is initialized before the {@link JpaEventStoreAutoConfiguration}.
 *
 * @author oliver.libutzki
 *
 */
@Configuration
@RegisterDefaultEntities( packages = {
		"org.axonframework.eventsourcing.eventstore.jpa"
} )
class EmbeddedServerJpaEventStoreConfiguration {
	@Bean
	public EventStorageEngine eventStorageEngine( final Serializer defaultSerializer,
			final PersistenceExceptionResolver persistenceExceptionResolver,
			@Qualifier( "eventSerializer" ) final Serializer eventSerializer,
			final AxonConfiguration configuration,
			final EntityManagerProvider entityManagerProvider,
			final TransactionManager transactionManager ) {
		return EmbeddedServerJpaEventStorageEngine.builder( )
				.snapshotSerializer( defaultSerializer )
				.upcasterChain( configuration.upcasterChain( ) )
				.persistenceExceptionResolver( persistenceExceptionResolver )
				.eventSerializer( eventSerializer )
				.entityManagerProvider( entityManagerProvider )
				.transactionManager( transactionManager )
				.build( );
	}
}
