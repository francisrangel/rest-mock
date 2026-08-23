package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

public class DelayTestCase extends IntegrationTestBase {

	@Test
	public void delayedResponseTakesAtLeastTheConfiguredTime() throws Exception {
		RestMock.whenGet("/slow").thenReturnText("done").withDelay(50);

		long start = System.currentTimeMillis();
		HttpResponse<String> response = sendRequest("/slow", HttpMethod.GET);
		long elapsed = System.currentTimeMillis() - start;

		assertEquals(200, response.statusCode());
		assertTrue(elapsed >= 50, "Expected at least 50ms but took " + elapsed + "ms");
	}

	// The "no delay configured means zero delay" invariant is verified deterministically by
	// HttpResponseForGETMethodTest.defaultDelayIsZero; asserting it here against the wall clock
	// made the test fail on a loaded machine without any defect.
	@Test
	public void aRouteWithoutDelayRespondsNormally() throws Exception {
		RestMock.whenGet("/fast").thenReturnText("done");

		HttpResponse<String> response = sendRequest("/fast", HttpMethod.GET);

		assertEquals(200, response.statusCode());
		assertEquals("done", response.body());
	}

}
