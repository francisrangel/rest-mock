package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMock;
import org.simulatest.restmock.RestMock;

/**
 * Manages the server itself rather than extending IntegrationTestBase: the
 * point of these tests is which port gets bound.
 */
public class ServerPortTestCase {

	private static final int OTHER_PORT = 9099;

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

		HttpResponse<String> response = TestHttp.get("http://localhost:" + bound + "/ping");

		assertEquals("pong", response.body());
	}

	/** Port 0 means "any", so it never conflicts with a server already up. */
	@Test
	public void portZeroIsAcceptedWhileRunning() {
		RestMock.startServer(OTHER_PORT);
		RestMock.startServer(0);

		assertEquals(OTHER_PORT, RestMock.port());
	}

	@Test
	public void baseUrlPointsAtTheBoundPort() {
		RestMock.startServer(OTHER_PORT);

		assertEquals("http://localhost:" + OTHER_PORT, RestMock.baseUrl());
		assertEquals("http://localhost:" + OTHER_PORT + "/users/42", RestMock.url("/users/42"));
	}

	/** A base URL for a server that is not listening would fail later, far from the cause. */
	@Test
	public void baseUrlWhileStoppedSaysToStartTheServer() {
		IllegalStateException failure = assertThrows(IllegalStateException.class, RestMock::baseUrl);

		assertTrue(failure.getMessage().contains("startServer"), failure.getMessage());
	}

	/** The README's first line: no port, and it is listening on 9080. */
	@Test
	public void startServerWithoutAPortBindsTheDefault() {
		RestMock.startServer();

		assertEquals(RestMock.DEFAULT_PORT, RestMock.port());
	}

	@Test
	public void anOwnMockAlsoDefaultsToThatPort() {
		HttpMock own = new HttpMock();
		try {
			own.startServer();

			assertEquals(RestMock.DEFAULT_PORT, own.port());
		} finally {
			own.stopServer();
		}
	}

	/** A port somebody else holds is reported, not swallowed, and the mock stays stopped. */
	@Test
	public void aPortAlreadyInUseFailsToBindAndLeavesTheMockStopped() throws Exception {
		try (ServerSocket occupied = new ServerSocket(OTHER_PORT)) {
			UncheckedIOException failure =
				assertThrows(UncheckedIOException.class, () -> RestMock.startServer(OTHER_PORT));

			assertInstanceOf(BindException.class, failure.getCause());
			assertEquals(-1, RestMock.port());
		}

		RestMock.startServer(OTHER_PORT);
		assertEquals(OTHER_PORT, RestMock.port(), "a failed start must not poison a later one");
	}

	@Test
	public void urlAcceptsAPathWithOrWithoutItsLeadingSlash() {
		RestMock.startServer(OTHER_PORT);

		assertEquals(RestMock.url("/ping"), RestMock.url("ping"));
		assertEquals(RestMock.baseUrl(), RestMock.url(""));
		assertEquals(RestMock.baseUrl(), RestMock.url(null));
	}

	@Test
	public void urlBuildsARequestThatActuallyReaches() throws Exception {
		RestMock.startServer(0);
		RestMock.whenGet("/ping").thenReturnText("pong");

		HttpResponse<String> response = TestHttp.get(RestMock.url("/ping"));

		assertEquals("pong", response.body());
	}

}
