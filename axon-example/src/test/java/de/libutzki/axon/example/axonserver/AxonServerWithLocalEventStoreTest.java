package de.libutzki.axon.example.axonserver;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.jupiter.api.Test;

import de.libutzki.axon.example.axonserver.TestConfiguration.ChildEventHandler;
import de.libutzki.axon.example.axonserver.TestConfiguration.GlobalTestEvent;
import de.libutzki.axon.example.axonserver.TestConfiguration.LocalTestEvent;

class AxonServerWithLocalEventStoreTest extends AbstractAxonServerIntegrationTest {

	@Override
	protected boolean useLocalEventStore( ) {
		return true;
	}

	@Test
	void testGlobalEvent( ) {
		final ChildEventHandler child1EventHandler = child1Context.getBean( ChildEventHandler.class );
		final ChildEventHandler child2EventHandler = child2Context.getBean( ChildEventHandler.class );
		final EventGateway child2EventGateway = child2Context.getBean( EventGateway.class );
		final GlobalTestEvent globalTestEvent = new GlobalTestEvent( "Test" );
		child2EventGateway.publish( globalTestEvent );
		verify( child1EventHandler, timeout( 3000 ) ).on( globalTestEvent );
		verify( child2EventHandler, timeout( 3000 ) ).on( globalTestEvent );
	}

	@Test
	void testLocalEvent( ) {
		final ChildEventHandler child1EventHandler = child1Context.getBean( ChildEventHandler.class );
		final ChildEventHandler child2EventHandler = child2Context.getBean( ChildEventHandler.class );
		final EventGateway child2EventGateway = child2Context.getBean( EventGateway.class );
		final LocalTestEvent localTestEvent = new LocalTestEvent( "Test" );
		child2EventGateway.publish( localTestEvent );
		verify( child2EventHandler, timeout( 3000 ) ).on( localTestEvent );
		verify( child1EventHandler, never( ) ).on( localTestEvent );
	}

}
