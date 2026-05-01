package restmock.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import restmock.http.HttpMethod;
import restmock.response.TextPlain;

public class RouteManagerTest {

	@AfterEach
	public void cleanUp() {
		RouteManager.getInstance().clean();
	}

	@Test
	public void literalPathBeatsTemplate() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("template"));
		manager.registerRoute(new Route(HttpMethod.GET, "/users/me"), new TextPlain("literal"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users/me").orElseThrow();

		assertEquals("literal", match.response().getContent());
	}

	@Test
	public void templateMatchesWhenLiteralDoesNot() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("template"));
		manager.registerRoute(new Route(HttpMethod.GET, "/users/me"), new TextPlain("literal"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users/42").orElseThrow();

		assertEquals("template", match.response().getContent());
	}

	@Test
	public void fewerCapturesWinsRegardlessOfRegistrationOrder() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/a/{x}/{y}"), new TextPlain("two captures"));
		manager.registerRoute(new Route(HttpMethod.GET, "/a/{x}/fixed"), new TextPlain("one capture"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/a/1/fixed").orElseThrow();

		assertEquals("one capture", match.response().getContent());
	}

	@Test
	public void noMatchReturnsEmpty() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("ok"));

		Optional<RouteManager.Match> match = manager.lookup(HttpMethod.GET, "/posts");

		assertTrue(match.isEmpty());
	}

	@Test
	public void differentMethodDoesNotMatch() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/test"), new TextPlain("ok"));

		Optional<RouteManager.Match> match = manager.lookup(HttpMethod.POST, "/test");

		assertTrue(match.isEmpty());
	}

	@Test
	public void pathCapturesAreReturned() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("ok"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users/42").orElseThrow();

		assertEquals("42", match.pathCaptures().get("id"));
	}

	@Test
	public void cleanRemovesAllRoutes() {
		RouteManager manager = RouteManager.getInstance();
		manager.registerRoute(new Route(HttpMethod.GET, "/test"), new TextPlain("ok"));

		manager.clean();

		assertTrue(manager.lookup(HttpMethod.GET, "/test").isEmpty());
	}

}
