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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.libutzki.axon.example.axonserver.child1.Child1Config;
import de.libutzki.axon.example.axonserver.child2.Child2Config;
import de.libutzki.axon.example.axonserver.parent.ParentConfig;

@Testcontainers
public abstract class AbstractAxonServerIntegrationTest {

	private static final int AXON_HTTP_PORT = 8024;
	private static final int AXON_GRPC_PORT = 8124;

	@Container
	private final GenericContainer<?> axonServer = new GenericContainer<>( "axoniq/axonserver:latest" )
			.withExposedPorts( AXON_HTTP_PORT, AXON_GRPC_PORT )
			.waitingFor( Wait.forHttp( "/actuator/info" ).forPort( AXON_HTTP_PORT ) );

	protected ConfigurableApplicationContext child1Context;
	protected ConfigurableApplicationContext child2Context;
	private ConfigurableApplicationContext parentContext;

	@BeforeEach
	void init( ) {
		final String[] args = new String[0];

		final StandardEnvironment environment = new StandardEnvironment( );
		final Map<String, Object> properties = new LinkedHashMap<>( );
		properties.put( "axon.axonserver.servers", String.format( "%s:%s", axonServer.getContainerIpAddress( ), axonServer.getMappedPort( AXON_GRPC_PORT ) ) );
		properties.put( "axon.localeventstore.enabled", useLocalEventStore( ) );
		environment.getPropertySources( ).addFirst( new MapPropertySource( "Test Properties", properties ) );

		final SpringApplication parentApplication = new SpringApplication( ParentConfig.class );
		parentApplication.setEnvironment( environment );
		parentContext = parentApplication.run( args );

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
		parentContext.close( );
	}

	protected abstract boolean useLocalEventStore( );

}
