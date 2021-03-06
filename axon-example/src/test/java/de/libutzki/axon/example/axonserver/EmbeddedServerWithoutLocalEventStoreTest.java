package de.libutzki.axon.example.axonserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.jupiter.api.Test;

import de.libutzki.axon.embeddedserver.EmbeddedServer;
import de.libutzki.axon.example.axonserver.TestConfiguration.ChildEventHandler;
import de.libutzki.axon.example.axonserver.TestConfiguration.GlobalTestEvent;
import de.libutzki.axon.example.axonserver.TestConfiguration.LocalTestEvent;

class EmbeddedServerWithoutLocalEventStoreTest extends AbstractEmbeddedServerIntegrationTest {

	@Override
	protected boolean useLocalEventStore( ) {
		return false;
	}

	@Test
	void bothChildrenUseSameEmbeddedServerInstanceWithoutLocalEventStore( ) {
		final EmbeddedServer child1EmbeddedServer = child1Context.getBean( EmbeddedServer.class );
		final EmbeddedServer child2EmbeddedServer = child2Context.getBean( EmbeddedServer.class );
		assertThat( child1EmbeddedServer ).isSameAs( child2EmbeddedServer );
	}

	@Test
	void testGlobalEventWithoutLocalEventStore( ) {
		final ChildEventHandler child1EventHandler = child1Context.getBean( ChildEventHandler.class );
		final ChildEventHandler child2EventHandler = child2Context.getBean( ChildEventHandler.class );
		final EventGateway child2EventGateway = child2Context.getBean( EventGateway.class );
		final GlobalTestEvent globalTestEvent = new GlobalTestEvent( "Test" );
		child2EventGateway.publish( globalTestEvent );
		verify( child1EventHandler, timeout( 3000 ) ).on( globalTestEvent );
		verify( child2EventHandler, timeout( 3000 ) ).on( globalTestEvent );
	}

	@Test
	void testLocalEventWithoutLocalEventStore( ) {
		final ChildEventHandler child1EventHandler = child1Context.getBean( ChildEventHandler.class );
		final ChildEventHandler child2EventHandler = child2Context.getBean( ChildEventHandler.class );
		final EventGateway child2EventGateway = child2Context.getBean( EventGateway.class );
		final LocalTestEvent localTestEvent = new LocalTestEvent( "Test" );
		child2EventGateway.publish( localTestEvent );
		verify( child2EventHandler, timeout( 3000 ) ).on( localTestEvent );
		verify( child1EventHandler, timeout( 3000 ) ).on( localTestEvent );
	}

}
