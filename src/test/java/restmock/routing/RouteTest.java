package restmock.routing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import restmock.http.HttpMethod;

public class RouteTest {

	@Test
	public void buildRouteViaString() {
		Route route = new Route("GET", "www.google.com");

		assertEquals(HttpMethod.GET, route.getMethod());
		assertEquals("www.google.com", route.getUri());
	}

}
