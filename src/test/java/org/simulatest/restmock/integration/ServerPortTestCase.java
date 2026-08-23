package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.RestMock;

/**
 * Manages the server itself rather than extending IntegrationTestBase: the
 * point of these tests is which port gets bound.
 */
public class ServerPortTestCase {

	private static final int OTHER_PORT = 9099;

	private final HttpClient client = HttpClient.newHttpClient();

	@AfterEach
	void stopServer() {
		RestMock.stopServer();
	}

	@Test
	public void portIsMinusOneWhileStopped() {
		assertEquals(-1, RestMock.port());
	}

	@Test
	public void portReportsWhatWasBound() {
		RestMock.startServer(OTHER_PORT);

		assertEquals(OTHER_PORT, RestMock.port());
	}

	/**
	 * The regression: this used to return silently, leaving the caller's
	 * requests going to a port nothing was listening on.
	 */
	@Test
	public void startingOnADifferentPortWhileRunningIsRejected() {
		RestMock.startServer(OTHER_PORT);

		IllegalStateException failure =
			assertThrows(IllegalStateException.class, () -> RestMock.startServer(OTHER_PORT + 1));

		assertTrue(failure.getMessage().contains(String.valueOf(OTHER_PORT)), failure.getMessage());
		assertEquals(OTHER_PORT, RestMock.port(), "the running server should be untouched");
	}

	@Test
	public void startingAgainOnTheSamePortIsANoOp() {
		RestMock.startServer(OTHER_PORT);
		RestMock.startServer(OTHER_PORT);

		assertEquals(OTHER_PORT, RestMock.port());
	}

	@Test
	public void portZeroBindsAFreePortAndReportsIt() throws Exception {
		RestMock.startServer(0);

		int bound = RestMock.port();
		assertNotEquals(0, bound);
		assertNotEquals(-1, bound);

		RestMock.whenGet("/ping").thenReturnText("pong");

		HttpResponse<String> response = client.send(
			HttpRequest.newBuilder().uri(URI.create("http://localhost:" + bound + "/ping")).GET().build(),
			HttpResponse.BodyHandlers.ofString());

		assertEquals("pong", response.body());
	}

	/** Port 0 means "any", so it never conflicts with a server already up. */
	@Test
	public void portZeroIsAcceptedWhileRunning() {
		RestMock.startServer(OTHER_PORT);
		RestMock.startServer(0);

		assertEquals(OTHER_PORT, RestMock.port());
	}

}
