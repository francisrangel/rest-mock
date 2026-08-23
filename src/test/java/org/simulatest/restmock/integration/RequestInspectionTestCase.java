package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.ReceivedRequest;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class RequestInspectionTestCase extends IntegrationTestBase {

	@Test
	public void theServerRecordsEveryRequestItServes() throws Exception {
		RestMock.whenPost("/api/users").thenReturnJSON("{\"id\":1}");

		sendRequest("/api/users", HttpMethod.POST, ContentType.APPLICATION_JSON.type(), "{\"name\":\"Bob\"}");

		assertEquals(1, RestMock.requests().count());
		assertEquals(1, RestMock.requests().countForRoute(HttpMethod.POST, "/api/users"));
	}

	@Test
	public void theRecordedRequestCarriesTheBodyQueryAndHeaders() throws Exception {
		RestMock.whenPost("/api/users").thenReturnJSON("{\"id\":1}");

		sendRequest("/api/users?source=web", HttpMethod.POST, ContentType.APPLICATION_JSON.type(), "{\"name\":\"Bob\"}");

		ReceivedRequest received = RestMock.requests().lastForPath("/api/users").orElseThrow();

		assertEquals(HttpMethod.POST, received.method());
		assertEquals("/api/users", received.path());
		assertEquals("source=web", received.query());
		assertEquals("{\"name\":\"Bob\"}", received.body());
		assertEquals(ContentType.APPLICATION_JSON.type(), received.headers().get("Content-type").get(0));
	}

	@Test
	public void requestsAreRecordedEvenWhenNoRouteMatches() throws Exception {
		sendRequest("/never-stubbed", HttpMethod.GET);

		assertEquals(1, RestMock.requests().countForPath("/never-stubbed"));
	}

	@Test
	public void countsAccumulateAcrossRequestsInArrivalOrder() throws Exception {
		RestMock.whenGet("/ping").thenReturnText("pong");

		sendRequest("/ping?n=1", HttpMethod.GET);
		sendRequest("/ping?n=2", HttpMethod.GET);

		assertEquals(2, RestMock.requests().countForPath("/ping"));
		assertEquals("n=2", RestMock.requests().lastForPath("/ping").orElseThrow().query());
	}

	@Test
	public void theRequestLogStartsEmptyForEachTest() {
		assertTrue(RestMock.requests().isEmpty(),
			"RestMockExtension should have cleared the log between tests");
	}

}
