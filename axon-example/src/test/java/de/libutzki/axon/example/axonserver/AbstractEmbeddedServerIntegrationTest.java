package de.libutzki.axon.example.axonserver;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.libutzki.axon.embeddedserver.AxonEmbeddedServerConfiguration;
import de.libutzki.axon.example.axonserver.child1.Child1Config;
import de.libutzki.axon.example.axonserver.child2.Child2Config;
import de.libutzki.axon.example.axonserver.parent.ParentConfig;

@Testcontainers
public abstract class AbstractEmbeddedServerIntegrationTest {

	protected ConfigurableApplicationContext child1Context;
	protected ConfigurableApplicationContext child2Context;
	private ConfigurableApplicationContext embeddedServerConfig;
	private ConfigurableApplicationContext parentContext;

	@BeforeEach
	void init( ) {
		final String[] args = new String[0];

		final StandardEnvironment environment = new StandardEnvironment( );
		final Map<String, Object> properties = new LinkedHashMap<>( );
		properties.put( "axon.localeventstore.enabled", useLocalEventStore( ) );
		properties.put( "axon.axonserver.enabled", false );
		environment.getPropertySources( ).addFirst( new MapPropertySource( "Test Properties", properties ) );

		final SpringApplication parentApplication = new SpringApplication( ParentConfig.class );
		parentApplication.setEnvironment( environment );
		parentContext = parentApplication.run( args );

		final SpringApplication embeddedServerApplication = new SpringApplication( AxonEmbeddedServerConfiguration.class );
		embeddedServerApplication.addInitializers( new ParentContextApplicationContextInitializer( parentContext ) );
		embeddedServerConfig = embeddedServerApplication.run( args );

		final SpringApplication springApplication1 = new SpringApplication( TestConfiguration.class, Child1Config.class );
		springApplication1.addInitializers( new ParentContextApplicationContextInitializer( parentContext ) );
		child1Context = springApplication1.run( args );

		final SpringApplication springApplication2 = new SpringApplication( TestConfiguration.class, Child2Config.class );
		springApplication2.addInitializers( new ParentContextApplicationContextInitializer( parentContext ) );
		child2Context = springApplication2.run( args );
	}

	@AfterEach
	void cleanup( ) {
		child1Context.close( );
		child2Context.close( );
		embeddedServerConfig.close( );
		parentContext.close( );
	}

	protected abstract boolean useLocalEventStore( );

}
