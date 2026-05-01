package restmock.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import restmock.http.HttpMethod;

public class RouteTest {

	@Test
	public void buildRouteViaString() {
		Route route = new Route("GET", "www.google.com");

		assertEquals(HttpMethod.GET, route.getMethod());
		assertEquals("www.google.com", route.getUri());
	}

	@Test
	public void equalWhenSameMethodAndUri() {
		Route a = new Route(HttpMethod.GET, "/users");
		Route b = new Route(HttpMethod.GET, "/users");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void notEqualWhenDifferentMethod() {
		Route get = new Route(HttpMethod.GET, "/users");
		Route post = new Route(HttpMethod.POST, "/users");

		assertNotEquals(get, post);
	}

	@Test
	public void notEqualWhenDifferentUri() {
		Route a = new Route(HttpMethod.GET, "/users");
		Route b = new Route(HttpMethod.GET, "/posts");

		assertNotEquals(a, b);
	}

	@Test
	public void notEqualToNull() {
		Route route = new Route(HttpMethod.GET, "/users");

		assertNotEquals(route, null);
	}

	@Test
	public void notEqualToDifferentType() {
		Route route = new Route(HttpMethod.GET, "/users");

		assertNotEquals(route, "/users");
	}

	@Test
	public void equalToSelf() {
		Route route = new Route(HttpMethod.GET, "/users");

		assertEquals(route, route);
	}

}
