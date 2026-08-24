package org.simulatest.restmock.internal.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;

public class NoRouteReportTest {

	@Test
	public void namesTheRequestThatFoundNothing() {
		String report = describe(HttpMethod.GET, "/users/1", route(HttpMethod.POST, "/orders"));

		assertTrue(report.startsWith("No stub for GET /users/1"), report);
	}

	@Test
	public void listsEveryStubbedRoute() {
		String report = describe(HttpMethod.GET, "/nowhere",
			route(HttpMethod.GET, "/users/{id}"), route(HttpMethod.POST, "/orders"));

		assertTrue(report.contains("Stubbed routes:"), report);
		assertTrue(report.contains("GET     /users/{id}"), report);
		assertTrue(report.contains("POST    /orders"), report);
	}

	/** With nothing registered the useful answer is the call the test forgot to make. */
	@Test
	public void anEmptyRouteTableSuggestsTheStubToWrite() {
		String report = describe(HttpMethod.GET, "/users/1");

		assertTrue(report.contains("Nothing is stubbed"), report);
		assertTrue(report.contains("RestMock.whenGet(\"/users/1\")"), report);
	}

	@Test
	public void aPathStubbedUnderOtherVerbsIsReportedAsAWrongVerb() {
		String report = describe(HttpMethod.GET, "/users/1",
			route(HttpMethod.POST, "/users/1"), route(HttpMethod.DELETE, "/users/1"));

		assertTrue(report.contains("/users/1 is stubbed for POST, DELETE, not GET."), report);
	}

	/** A wrong verb is a certainty, so it must not be diluted by a spelling guess. */
	@Test
	public void theWrongVerbHintWinsOverTheSpellingHint() {
		String report = describe(HttpMethod.GET, "/users/1",
			route(HttpMethod.POST, "/users/1"), route(HttpMethod.GET, "/users/2"));

		assertFalse(report.contains("Closest stub"), report);
	}

	@Test
	public void aPathOneCharacterOffIsOfferedAsTheClosestStub() {
		String report = describe(HttpMethod.GET, "/users/01", route(HttpMethod.GET, "/users/1"));

		assertTrue(report.contains("Closest stub: GET /users/1"), report);
	}

	@Test
	public void aTemplateIsOfferedAsItWasWritten() {
		String report = describe(HttpMethod.GET, "/user/42", route(HttpMethod.GET, "/users/{id}"));

		assertTrue(report.contains("Closest stub: GET /users/{id}"), report);
	}

	@Test
	public void anUnrelatedPathIsNotOfferedAsAGuess() {
		String report = describe(HttpMethod.GET, "/billing/invoices", route(HttpMethod.GET, "/health"));

		assertFalse(report.contains("Closest stub"), report);
	}

	@Test
	public void aLongRouteTableIsTruncated() {
		List<Route> routes = new ArrayList<>();
		for (int i = 0; i < 25; i++) routes.add(route(HttpMethod.GET, "/resource/" + i));

		String report = NoRouteReport.describe(HttpMethod.POST, "/nowhere", routes);

		assertTrue(report.contains("and 5 more"), report);
	}

	private static String describe(HttpMethod method, String path, Route... routes) {
		return NoRouteReport.describe(method, path, List.of(routes));
	}

	private static Route route(HttpMethod method, String uri) {
		return new Route(method, uri);
	}

}
