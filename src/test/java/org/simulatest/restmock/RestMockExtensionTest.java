package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.simulatest.restmock.internal.response.TextPlain;
import org.simulatest.restmock.internal.routing.Route;

public class RestMockExtensionTest {

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

	@Test
	public void afterEachCleansRoutesAndRequestsByDefault() throws Exception {
		RestMockExtension extension = new RestMockExtension();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));
		record("/test");

		extension.afterEach(mock(ExtensionContext.class));

		assertNull(RestMock.routeManager().get(route));
		assertTrue(RestMock.requests().isEmpty());
	}

	@Test
	public void afterEachPreservesRoutesAndRequestsWhenKeepRoutes() throws Exception {
		RestMockExtension extension = new RestMockExtension().keepRoutes();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));
		record("/test");

		extension.afterEach(mock(ExtensionContext.class));

		assertNotNull(RestMock.routeManager().get(route));
		assertFalse(RestMock.requests().isEmpty());
	}

}
