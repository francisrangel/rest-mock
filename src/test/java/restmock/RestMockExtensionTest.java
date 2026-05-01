package restmock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import restmock.http.HttpMethod;
import restmock.routing.Route;
import restmock.routing.RouteManager;
import restmock.response.TextPlain;

public class RestMockExtensionTest {

	@Test
	public void afterEachCleansRoutesByDefault() throws Exception {
		RestMockExtension extension = new RestMockExtension();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));

		extension.afterEach(null);

		assertNull(RestMock.routeManager().get(route));
	}

	@Test
	public void afterEachPreservesRoutesWhenKeepRoutes() throws Exception {
		RestMockExtension extension = new RestMockExtension().keepRoutes();
		Route route = new Route(HttpMethod.GET, "/test");
		RestMock.routeManager().registerRoute(route, new TextPlain("ok"));

		extension.afterEach(null);

		assertNotNull(RestMock.routeManager().get(route));

		RestMock.clean();
	}

}
