package org.simulatest.restmock.internal.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.internal.response.Template;
import org.simulatest.restmock.internal.response.TextPlain;

public class RouteManagerTest {

	private static String body(RouteManager.Match match) {
		return assertInstanceOf(Template.class, match.response()).getContent();
	}

	@Test
	public void literalPathBeatsTemplate() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("template"));
		manager.registerRoute(new Route(HttpMethod.GET, "/users/me"), new TextPlain("literal"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users/me").orElseThrow();

		assertEquals("literal", body(match));
	}

	@Test
	public void templateMatchesWhenLiteralDoesNot() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("template"));
		manager.registerRoute(new Route(HttpMethod.GET, "/users/me"), new TextPlain("literal"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users/42").orElseThrow();

		assertEquals("template", body(match));
	}

	@Test
	public void fewerCapturesWinsRegardlessOfRegistrationOrder() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/a/{x}/{y}"), new TextPlain("two captures"));
		manager.registerRoute(new Route(HttpMethod.GET, "/a/{x}/fixed"), new TextPlain("one capture"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/a/1/fixed").orElseThrow();

		assertEquals("one capture", body(match));
	}

	@Test
	public void noMatchReturnsEmpty() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("ok"));

		Optional<RouteManager.Match> match = manager.lookup(HttpMethod.GET, "/posts");

		assertTrue(match.isEmpty());
	}

	@Test
	public void differentMethodDoesNotMatch() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/test"), new TextPlain("ok"));

		Optional<RouteManager.Match> match = manager.lookup(HttpMethod.POST, "/test");

		assertTrue(match.isEmpty());
	}

	@Test
	public void pathCapturesAreReturned() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("ok"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users/42").orElseThrow();

		assertEquals("42", match.pathCaptures().get("id"));
	}

	@Test
	public void pathCapturesCannotBeModifiedByCallers() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("ok"));

		Map<String, String> captures = manager.lookup(HttpMethod.GET, "/users/42").orElseThrow().pathCaptures();

		assertThrows(UnsupportedOperationException.class, () -> captures.put("id", "99"));
	}

	@Test
	public void methodsForCollectsEveryMethodRegisteredForThePath() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("ok"));
		manager.registerRoute(new Route(HttpMethod.POST, "/users"), new TextPlain("ok"));
		manager.registerRoute(new Route(HttpMethod.DELETE, "/posts"), new TextPlain("ok"));

		assertEquals(Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.HEAD, HttpMethod.OPTIONS), manager.methodsFor("/users"));
	}

	@Test
	public void methodsForMatchesTemplatedRoutes() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.PUT, "/users/{id}"), new TextPlain("ok"));

		assertEquals(Set.of(HttpMethod.PUT, HttpMethod.OPTIONS), manager.methodsFor("/users/42"));
	}

	/** A path stubbed only for POST answers OPTIONS, but must not claim HEAD. */
	@Test
	public void methodsForAdvertisesHeadOnlyWhereGetIsStubbed() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.POST, "/submit"), new TextPlain("ok"));

		assertEquals(Set.of(HttpMethod.POST, HttpMethod.OPTIONS), manager.methodsFor("/submit"));
	}

	@Test
	public void headIsAnsweredByTheGetRoute() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users/{id}"), new TextPlain("from get"));

		RouteManager.Match match = manager.lookup(HttpMethod.HEAD, "/users/42").orElseThrow();

		assertEquals("from get", body(match));
		assertEquals(Map.of("id", "42"), match.pathCaptures());
	}

	@Test
	public void anExplicitHeadStubWinsOverTheGetRoute() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("from get"));
		manager.registerRoute(new Route(HttpMethod.HEAD, "/users"), new TextPlain("from head"));

		assertEquals("from head", body(manager.lookup(HttpMethod.HEAD, "/users").orElseThrow()));
	}

	@Test
	public void headIsNotAnsweredByAnyOtherMethod() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.POST, "/users"), new TextPlain("ok"));

		assertTrue(manager.lookup(HttpMethod.HEAD, "/users").isEmpty());
	}

	@Test
	public void methodsForIsEmptyWhenNothingMatchesThePath() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("ok"));

		assertTrue(manager.methodsFor("/posts").isEmpty());
	}

	@Test
	public void theLastRegisteredRouteWinsWhenCaptureCountsTie() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/t/{x}/c"), new TextPlain("first"));
		manager.registerRoute(new Route(HttpMethod.GET, "/t/b/{y}"), new TextPlain("second"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/t/b/c").orElseThrow();

		assertEquals("second", body(match));
	}

	@Test
	public void reRegisteringTheSameRouteReplacesTheResponse() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("first"));
		manager.registerRoute(new Route(HttpMethod.GET, "/users"), new TextPlain("second"));

		RouteManager.Match match = manager.lookup(HttpMethod.GET, "/users").orElseThrow();

		assertEquals("second", body(match));
		assertEquals(1, manager.registeredRoutes().size());
	}

	@Test
	public void cleanRemovesAllRoutes() {
		RouteManager manager = new RouteManager();
		manager.registerRoute(new Route(HttpMethod.GET, "/test"), new TextPlain("ok"));

		manager.clean();

		assertTrue(manager.lookup(HttpMethod.GET, "/test").isEmpty());
	}

}
