package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.http.HttpResponse;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.RestMock;

/**
 * Manages the server itself rather than extending IntegrationTestBase: the
 * point of these tests is which port gets bound. Each test picks a port that
 * is free right now, so nothing here fights another build for a fixed one.
 */
public class ServerPortTestCase {

	private int port;

	@BeforeEach
	void pickAFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
	}

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
		RestMock.startServer(port);

		assertEquals(port, RestMock.port());
	}

	/**
	 * The regression: this used to return silently, leaving the caller's
	 * requests going to a port nothing was listening on.
	 */
	@Test
	public void startingOnADifferentPortWhileRunningIsRejected() {
		RestMock.startServer(port);

		IllegalStateException failure =
			assertThrows(IllegalStateException.class, () -> RestMock.startServer(port + 1));

		assertTrue(failure.getMessage().contains(String.valueOf(port)), failure.getMessage());
		assertEquals(port, RestMock.port(), "the running server should be untouched");
	}

	@Test
	public void startingAgainOnTheSamePortIsANoOp() {
		RestMock.startServer(port);
		RestMock.startServer(port);

		assertEquals(port, RestMock.port());
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
		RestMock.startServer(port);
		RestMock.startServer(0);

		assertEquals(port, RestMock.port());
	}

	@Test
	public void baseUrlPointsAtTheBoundPort() {
		RestMock.startServer(port);

		assertEquals("http://localhost:" + port, RestMock.baseUrl());
		assertEquals("http://localhost:" + port + "/users/42", RestMock.url("/users/42"));
	}

	/** A base URL for a server that is not listening would fail later, far from the cause. */
	@Test
	public void baseUrlWhileStoppedSaysToStartTheServer() {
		IllegalStateException failure = assertThrows(IllegalStateException.class, RestMock::baseUrl);

		assertTrue(failure.getMessage().contains("startServer"), failure.getMessage());
	}

	/** The README's first line: no port, and it is listening on one the OS picked. */
	@Test
	public void startServerWithoutAPortBindsAnOsAssignedOne() {
		RestMock.startServer();

		assertTrue(RestMock.port() > 0);
	}

	/** A port somebody else holds is reported, not swallowed, and the mock stays stopped. */
	@Test
	public void aPortAlreadyInUseFailsToBindAndLeavesTheMockStopped() throws Exception {
		try (ServerSocket occupied = new ServerSocket(port)) {
			UncheckedIOException failure =
				assertThrows(UncheckedIOException.class, () -> RestMock.startServer(port));

			assertInstanceOf(BindException.class, failure.getCause());
			assertEquals(-1, RestMock.port());
		}

		RestMock.startServer(port);
		assertEquals(port, RestMock.port(), "a failed start must not poison a later one");
	}

	/**
	 * The server used to bind the wildcard address, so every stub was reachable
	 * from the network while baseUrl() said localhost. Refused and timed out
	 * both mean unreachable; which one depends on the firewall. Skipped on a
	 * machine with no non-loopback interface, where there is no address to try.
	 */
	@Test
	public void theServerIsNotReachableFromANonLoopbackAddress() throws Exception {
		InetAddress external = aNonLoopbackAddress();
		assumeTrue(external != null, "no non-loopback interface on this machine");

		RestMock.startServer();

		try (Socket socket = new Socket()) {
			assertThrows(IOException.class,
				() -> socket.connect(new InetSocketAddress(external, RestMock.port()), 1000),
				"the mock accepted a connection on " + external + "; it should listen on loopback only");
		}
	}

	@Test
	public void urlAcceptsAPathWithOrWithoutItsLeadingSlash() {
		RestMock.startServer(port);

		assertEquals(RestMock.url("/ping"), RestMock.url("ping"));
		assertEquals(RestMock.baseUrl(), RestMock.url(""));
		assertEquals(RestMock.baseUrl(), RestMock.url(null));
	}

	@Test
	public void urlBuildsARequestThatActuallyReaches() throws Exception {
		RestMock.startServer();
		RestMock.whenGet("/ping").thenReturnText("pong");

		HttpResponse<String> response = TestHttp.get(RestMock.url("/ping"));

		assertEquals("pong", response.body());
	}

	private static InetAddress aNonLoopbackAddress() throws SocketException {
		for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
			if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;

			for (InetAddress address : Collections.list(networkInterface.getInetAddresses()))
				if (address instanceof Inet4Address) return address;
		}
		return null;
	}

}
