package de.libutzki.axon.example.axonserver;

import static org.mockito.Mockito.mock;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.libutzki.axon.localeventstore.GlobalEvent;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Configuration
public class TestConfiguration {
	@Bean
	ChildEventHandler childEventHandler( ) {
		return mock( ChildEventHandler.class );
	}

	static class ChildEventHandler {

		@EventHandler
		void on( final GlobalTestEvent globalTestEvent ) {
		}

		@EventHandler
		void on( final LocalTestEvent localTestEvent ) {
		}
	}

	@RequiredArgsConstructor
	@Value
	@GlobalEvent
	static class GlobalTestEvent {
		private final String payload;
	}

	@RequiredArgsConstructor
	@Value
	static class LocalTestEvent {
		private final String payload;
	}
}
