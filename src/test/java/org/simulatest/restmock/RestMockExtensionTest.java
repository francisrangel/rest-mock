package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.integration.TestHttp;

/** Drives the callbacks by hand, without a JUnit context, against a mock the test owns. */
public class RestMockExtensionTest {

	private final HttpMock own = new HttpMock();

	@AfterEach
	public void stopEverything() {
		own.stopServer();
		RestMock.stopServer();
	}

	/** Two modules testing on one CI agent must not fight over a port, so no port means an OS-assigned one. */
	@Test
	public void theDefaultExtensionDrivesTheDefaultMockOnAnOsAssignedPort() {
		new RestMockExtension().beforeAll(null);

		assertTrue(RestMock.port() > 0);
	}

	@Test
	public void aGivenPortIsTheOneBound() throws Exception {
		int port = freePort();

		new RestMockExtension(port).beforeAll(null);

		assertEquals(port, RestMock.port());
	}

	/** The field a test registers is the thing it stubs on, whichever mock it drives. */
	@Test
	public void theExtensionStubsAndAddressesTheMockItDrives() throws Exception {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);

		extension.whenGet("/own").thenReturnText("ok");

		assertEquals("ok", TestHttp.get(own.url("/own")).body());
		assertSame(own.requests(), extension.requests());
		assertEquals(own.url("/own"), extension.url("/own"));
	}

	@Test
	public void aNullMockIsRejectedUpFront() {
		assertThrows(NullPointerException.class, () -> new RestMockExtension((HttpMock) null));
	}

	@Test
	public void afterEachCleansRoutesAndRequestsByDefault() throws Exception {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);
		extension.whenGet("/test").thenReturnText("ok");
		TestHttp.get(own.url("/test"));

		extension.afterEach(null);

		assertTrue(own.requests().isEmpty());
		assertEquals(404, TestHttp.get(own.url("/test")).statusCode());
	}

	@Test
	public void afterEachPreservesRoutesAndRequestsWhenKeepRoutes() throws Exception {
		RestMockExtension extension = new RestMockExtension(own).keepRoutes();
		extension.beforeAll(null);
		extension.whenGet("/test").thenReturnText("ok");
		TestHttp.get(own.url("/test"));

		extension.afterEach(null);

		assertFalse(own.requests().isEmpty());
		assertEquals(200, TestHttp.get(own.url("/test")).statusCode());
	}

	@Test
	public void afterAllStopsTheServer() {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);

		extension.afterAll(null);

		assertEquals(-1, own.port());
	}

	/** A port nothing holds right now, so the next bind gets it. */
	private static int freePort() throws Exception {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

}
