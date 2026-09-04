package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.internal.response.TextPlain;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;

public class RestMockExtensionTest {

	private final RouteManager routes = RestMock.defaultMock().routeManager();

	// This class mutates the process-wide RestMock state directly, so the reset has
	// to run even when an assertion fails - otherwise the route leaks into every
	// later test class in the same JVM. Stopping also cleans, and is a no-op when
	// nothing was started.
	@AfterEach
	public void resetGlobalState() {
		RestMock.stopServer();
	}

	private void record(String path) {
		RestMock.requests().add(
			new ReceivedRequest(HttpMethod.GET, path, null, Map.of(), "", Instant.now()));
	}

	@Test
	public void anExtensionExposesTheMockItDrives() {
		HttpMock own = new HttpMock();

		assertSame(own, new RestMockExtension(own).mock());
		assertSame(RestMock.defaultMock(), new RestMockExtension().mock());
	}

	/** Two modules testing on one CI agent must not fight over a port, so no port means an OS-assigned one. */
	@Test
	public void theDefaultExtensionBindsAnOsAssignedPort() {
		new RestMockExtension().beforeAll(null);

		assertTrue(RestMock.port() > 0);
	}

	@Test
	public void aNullMockIsRejectedUpFront() {
		assertThrows(NullPointerException.class, () -> new RestMockExtension((HttpMock) null));
	}

	/** The extension never reads the context, so the callbacks are driven without one. */
	@Test
	public void afterEachCleansRoutesAndRequestsByDefault() {
		RestMockExtension extension = new RestMockExtension();
		Route route = new Route(HttpMethod.GET, "/test");
		routes.registerRoute(route, new TextPlain("ok"));
		record("/test");

		extension.afterEach(null);

		assertNull(routes.get(route));
		assertTrue(RestMock.requests().isEmpty());
	}

	@Test
	public void afterEachPreservesRoutesAndRequestsWhenKeepRoutes() {
		RestMockExtension extension = new RestMockExtension().keepRoutes();
		Route route = new Route(HttpMethod.GET, "/test");
		routes.registerRoute(route, new TextPlain("ok"));
		record("/test");

		extension.afterEach(null);

		assertNotNull(routes.get(route));
		assertFalse(RestMock.requests().isEmpty());
	}

}
