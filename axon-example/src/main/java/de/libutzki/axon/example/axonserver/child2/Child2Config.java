package de.libutzki.axon.example.axonserver.child2;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import de.libutzki.axon.example.axonserver.child.ChildConfig;

@Configuration
@ComponentScan
@Import( ChildConfig.class )
@PropertySource( "classpath:/de/libutzki/axon/example/axonserver/child2/application.properties" )
public class Child2Config {

}
