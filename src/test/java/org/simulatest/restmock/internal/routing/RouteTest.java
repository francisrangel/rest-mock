package org.simulatest.restmock.internal.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;

public class RouteTest {

	/** Routes are the route table's keys, so a second stub for the same method and URI must land on the first. */
	@Test
	public void equalityIsByMethodAndUri() {
		Route route = new Route(HttpMethod.GET, "/users");

		assertEquals(route, new Route(HttpMethod.GET, "/users"));
		assertEquals(route.hashCode(), new Route(HttpMethod.GET, "/users").hashCode());
		assertNotEquals(route, new Route(HttpMethod.POST, "/users"));
		assertNotEquals(route, new Route(HttpMethod.GET, "/posts"));
	}

	@Test
	public void matchesPathIgnoresTheMethod() {
		Route route = new Route(HttpMethod.POST, "/users/{id}");

		assertTrue(route.matchesPath("/users/42"));
		assertTrue(route.matchesPath("/users/me"));
	}

	@Test
	public void matchesPathIsAnchoredAtBothEnds() {
		Route route = new Route(HttpMethod.GET, "/users/{id}");

		assertFalse(route.matchesPath("/users/42/roles"));
		assertFalse(route.matchesPath("/api/users/42"));
		assertFalse(route.matchesPath("/users/"));
	}

	@Test
	public void matchRequiresTheSameMethod() {
		Route route = new Route(HttpMethod.GET, "/users");

		assertTrue(route.match(HttpMethod.GET, "/users").isPresent());
		assertTrue(route.match(HttpMethod.POST, "/users").isEmpty());
	}

	@Test
	public void matchCapturesPlaceholderValuesInOrder() {
		Route route = new Route(HttpMethod.GET, "/users/{id}/posts/{postId}");

		Map<String, String> captures = route.match(HttpMethod.GET, "/users/42/posts/7").orElseThrow();

		assertEquals(2, route.captureCount());
		assertEquals("42", captures.get("id"));
		assertEquals("7", captures.get("postId"));
	}

	@Test
	public void aLiteralRouteMatchesWithNoCaptures() {
		Route route = new Route(HttpMethod.GET, "/users");

		assertEquals(0, route.captureCount());
		assertEquals(Map.of(), route.match(HttpMethod.GET, "/users").orElseThrow());
	}

	@Test
	public void placeholdersDoNotSpanPathSegments() {
		Route route = new Route(HttpMethod.GET, "/files/{name}");

		assertTrue(route.match(HttpMethod.GET, "/files/report.pdf").isPresent());
		assertTrue(route.match(HttpMethod.GET, "/files/2024/report.pdf").isEmpty());
	}

	@Test
	public void regexCharactersInTheUriAreTreatedLiterally() {
		Route route = new Route(HttpMethod.GET, "/a.b");

		assertTrue(route.matchesPath("/a.b"));
		assertFalse(route.matchesPath("/axb"));
	}

	@Test
	public void aUriWithoutALeadingSlashIsRejected() {
		assertRejected("users/1", "must start with '/'");
	}

	@Test
	public void aUriCarryingAQueryStringIsRejected() {
		assertRejected("/users?active=true", "must not contain a query string");
	}

	/** The message has to name the path to stub instead, or the fix is a guess. */
	@Test
	public void theQueryStringMessagePointsAtThePathToStub() {
		IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
			() -> new Route(HttpMethod.GET, "/users?active=true"));

		assertTrue(rejected.getMessage().contains("\"/users\""), rejected.getMessage());
		assertTrue(rejected.getMessage().contains("${name}"), rejected.getMessage());
	}

	@Test
	public void aUriCarryingAFragmentIsRejected() {
		assertRejected("/users#top", "must not contain a fragment");
	}

	/** Unclosed, unopened, empty, spanning a segment, nested: one rule catches them all. */
	@Test
	public void aMalformedPlaceholderIsRejected() {
		for (String uri : new String[] {"/users/{id", "/users/id}", "/users/{}", "/users/{a/b}", "/users/{a{b}}"})
			assertRejected(uri, "malformed placeholder");
	}

	@Test
	public void aRepeatedPlaceholderNameIsRejected() {
		assertRejected("/tenants/{id}/users/{id}", "more than once");
	}

	@Test
	public void aValidTemplateIsAccepted() {
		Route route = new Route(HttpMethod.GET, "/tenants/{tenant}/users/{id}");

		assertEquals(2, route.captureCount());
		assertTrue(route.matchesPath("/tenants/acme/users/1"));
	}

	private static void assertRejected(String uri, String expectedFragment) {
		IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
			() -> new Route(HttpMethod.GET, uri));

		assertTrue(rejected.getMessage().contains(expectedFragment),
			"message should explain the problem, but was: " + rejected.getMessage());
		assertTrue(rejected.getMessage().contains(uri),
			"message should quote the offending uri, but was: " + rejected.getMessage());
	}

}
