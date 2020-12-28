package de.libutzki.axon.example.axonserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import de.libutzki.axon.example.axonserver.child1.Child1Config;
import de.libutzki.axon.example.axonserver.child2.Child2Config;
import de.libutzki.axon.example.axonserver.parent.ParentConfig;

public class AxonServerExampleApplication {

	public static void main( final String[] args ) {

		final SpringApplication parentApplication = new SpringApplication( ParentConfig.class );
		final ConfigurableApplicationContext parentContext = parentApplication.run( args );
		final SpringApplication springApplication1 = new SpringApplication( Child1Config.class );
		final SpringApplication springApplication2 = new SpringApplication( Child2Config.class );
		springApplication1.addInitializers( new ParentContextApplicationContextInitializer( parentContext ) );
		springApplication2.addInitializers( new ParentContextApplicationContextInitializer( parentContext ) );
		final ConfigurableApplicationContext child1Context = springApplication1.run( args );
		final ConfigurableApplicationContext child2Context = springApplication2.run( args );
		System.out.println( "Child1-Applicationame" + child1Context.getApplicationName( ) );
		System.out.println( "Child2-Applicationame" + child2Context.getApplicationName( ) );

	}

}