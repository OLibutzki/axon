package de.libutzki.axon.example.axonserver.child;

import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@EnableAutoConfiguration
@ComponentScan( excludeFilters = { @Filter( type = FilterType.CUSTOM, classes = TypeExcludeFilter.class ),
		@Filter( type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class ) } )
public class ChildConfig {

}
