package de.libutzki.axon.embeddedserver;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jpa.SQLErrorCodesResolver;
import org.axonframework.springboot.util.jpa.ContainerManagedEntityManagerProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@EnableAutoConfiguration( excludeName = { "de.libutzki.axon.localeventstore.AxonLocalEventStoreConfiguration", "de.libutzki.axon.embeddedserverconnector.EmbeddedServerConnectorConfiguration" } )
@ComponentScan
@ConditionalOnProperty( name = "axon.embeddedserver.enabled", matchIfMissing = true )
@EntityScan( { "org.axonframework.eventhandling.tokenstore", "org.axonframework.modelling.saga.repository.jpa", "org.axonframework.eventsourcing.eventstore.jpa" } )
@PropertySource( "classpath:/de/libutzki/axon/embeddedserver/embeddedserver.properties" )
public class AxonEmbeddedServerConfiguration {

	@Bean
	public EmbeddedServer embeddedServer( final EmbeddedServerJpaEventStorageEngine eventStorageEngine, final EventStore eventStore ) {
		return new DefaultEmbeddedServer( eventStorageEngine, eventStore );
	}

	// The entityManagerProvider and persistenceExceptionResolver would usually be initialized in the JpaAutoConfiguration,
	// thus we need to initialize them by ourselves.
	@ConditionalOnMissingBean
	@Bean
	public EntityManagerProvider entityManagerProvider( ) {
		return new ContainerManagedEntityManagerProvider( );
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean( DataSource.class )
	public PersistenceExceptionResolver persistenceExceptionResolver( final DataSource dataSource )
			throws SQLException {
		return new SQLErrorCodesResolver( dataSource );
	}

}
