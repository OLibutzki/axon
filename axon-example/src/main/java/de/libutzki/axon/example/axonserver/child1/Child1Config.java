package de.libutzki.axon.example.axonserver.child1;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import de.libutzki.axon.example.axonserver.child.ChildConfig;

@Configuration
@ComponentScan
@Import( ChildConfig.class )
@PropertySource( "classpath:/de/libutzki/axon/example/axonserver/child1/application.properties" )
public class Child1Config {

}
