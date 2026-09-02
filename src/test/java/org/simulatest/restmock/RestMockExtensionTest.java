package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
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

	/** The extension only ever asks the context for a display name to log. */
	private static ExtensionContext context() {
		return (ExtensionContext) Proxy.newProxyInstance(
			ExtensionContext.class.getClassLoader(),
			new Class<?>[] { ExtensionContext.class },
			(proxy, method, args) -> method.getName().equals("getDisplayName") ? "a test" : null);
	}

	private void record(String path) {
		RestMock.requests().add(
			new ReceivedRequest(HttpMethod.GET, path, null, Map.of(), "", Instant.now()));
	}

	@Test
	public void afterEachCleansRoutesAndRequestsByDefault() {
		RestMockExtension extension = new RestMockExtension();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));
		record("/test");

		extension.afterEach(context());

		assertNull(RestMock.routeManager().get(route));
		assertTrue(RestMock.requests().isEmpty());
	}

	@Test
	public void afterEachPreservesRoutesAndRequestsWhenKeepRoutes() {
		RestMockExtension extension = new RestMockExtension().keepRoutes();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));
		record("/test");

		extension.afterEach(context());

		assertNotNull(RestMock.routeManager().get(route));
		assertFalse(RestMock.requests().isEmpty());
	}

}
