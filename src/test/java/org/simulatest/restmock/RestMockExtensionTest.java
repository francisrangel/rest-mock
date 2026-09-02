package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	// later test class in the same JVM.
	@AfterEach
	public void resetGlobalState() {
		RestMock.clean();
	}

	private void record(String path) {
		RestMock.requests().add(
			new ReceivedRequest(HttpMethod.GET, path, null, Map.of(), "", Instant.now()));
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
