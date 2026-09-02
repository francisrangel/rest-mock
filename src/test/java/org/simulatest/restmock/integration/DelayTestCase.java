package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

public class DelayTestCase extends IntegrationTestBase {

	@Test
	public void delayedResponseTakesAtLeastTheConfiguredTime() throws Exception {
		RestMock.whenGet("/slow").thenReturnText("done").withDelay(50);

		assertTakesAtLeast(50, "/slow");
	}

	// The "no delay configured means zero delay" invariant is verified deterministically by
	// ResponseOptionsTest.defaultDelayIsZero; asserting it here against the wall clock
	// made the test fail on a loaded machine without any defect.
	@Test
	public void aRouteWithoutDelayRespondsNormally() throws Exception {
		RestMock.whenGet("/fast").thenReturnText("done");

		HttpResponse<String> response = sendRequest("/fast", HttpMethod.GET);

		assertEquals(200, response.statusCode());
		assertEquals("done", response.body());
	}

	/** withDelay(2000) does not say whether it means milliseconds; a Duration does. */
	@Test
	public void aDelayCanBeStatedAsADuration() throws Exception {
		RestMock.whenGet("/slow-duration").thenReturnText("done").withDelay(Duration.ofMillis(50));

		assertTakesAtLeast(50, "/slow-duration");
	}

	private void assertTakesAtLeast(long millis, String path) throws Exception {
		long start = System.currentTimeMillis();
		HttpResponse<String> response = sendRequest(path, HttpMethod.GET);
		long elapsed = System.currentTimeMillis() - start;

		assertEquals(200, response.statusCode());
		assertTrue(elapsed >= millis, "Expected at least " + millis + "ms but took " + elapsed + "ms");
	}

	@Test
	public void aNegativeDelayIsRejectedRatherThanIgnored() {
		assertThrows(IllegalArgumentException.class,
			() -> RestMock.whenGet("/backwards").thenReturnText("done").withDelay(-1));
	}

}
