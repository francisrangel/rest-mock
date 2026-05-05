package restmock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import restmock.internal.response.TextPlain;
import restmock.internal.routing.Route;

public class RestMockExtensionTest {

	@Test
	public void afterEachCleansRoutesByDefault() throws Exception {
		RestMockExtension extension = new RestMockExtension();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));

		extension.afterEach(mock(ExtensionContext.class));

		assertNull(RestMock.routeManager().get(route));
	}

	@Test
	public void afterEachPreservesRoutesWhenKeepRoutes() throws Exception {
		RestMockExtension extension = new RestMockExtension().keepRoutes();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));

		extension.afterEach(mock(ExtensionContext.class));

		assertNotNull(RestMock.routeManager().get(route));

		RestMock.clean();
	}

}
