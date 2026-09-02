package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

/** The second most common test after the happy path: the upstream failed once. */
public class SequencedResponsesTestCase extends IntegrationTestBase {

	@Test
	public void aRouteServesItsResponsesInOrderAndRepeatsTheLast() throws Exception {
		RestMock.whenGet("/flaky")
			.thenReturnErrorCodeWithMessage(503, "down")
			.thenReturnText("up");

		assertEquals(503, sendRequest("/flaky", HttpMethod.GET).statusCode());
		assertEquals("up", sendRequest("/flaky", HttpMethod.GET).body());
		assertEquals("up", sendRequest("/flaky", HttpMethod.GET).body());
	}

	@Test
	public void optionsApplyToTheResponseTheyFollow() throws Exception {
		RestMock.whenGet("/flaky")
			.thenReturnText("first").withStatus(500)
			.thenReturnText("second");

		assertEquals(500, sendRequest("/flaky", HttpMethod.GET).statusCode());
		assertEquals(200, sendRequest("/flaky", HttpMethod.GET).statusCode());
	}

	/** A new when*() for the same route replaces the sequence rather than extending it. */
	@Test
	public void stubbingTheRouteAgainStartsOver() throws Exception {
		RestMock.whenGet("/flaky").thenReturnText("a").thenReturnText("b");
		sendRequest("/flaky", HttpMethod.GET);

		RestMock.whenGet("/flaky").thenReturnText("c");

		assertEquals("c", sendRequest("/flaky", HttpMethod.GET).body());
	}

}
