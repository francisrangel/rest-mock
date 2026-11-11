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
	public void aStubMadeInATestIsGoneAfterItAlongWithItsRequests() throws Exception {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);
		extension.beforeEach(null);
		extension.whenGet("/test").thenReturnText("ok");
		TestHttp.get(own.url("/test"));

		extension.afterEach(null);

		assertTrue(own.requests().isEmpty());
		assertEquals(404, TestHttp.get(own.url("/test")).statusCode());
	}

	/** What a @BeforeAll stubs is the class's fixture, so the reset between tests leaves it alone. */
	@Test
	public void aStubMadeBeforeTheTestsOutlivesThem() throws Exception {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);
		extension.whenGet("/shared").thenReturnText("ok");
		extension.beforeEach(null);

		extension.afterEach(null);

		assertEquals(200, TestHttp.get(own.url("/shared")).statusCode());
	}

	@Test
	public void aClassStubRemovedByCleanIsBackForTheNextTest() throws Exception {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);
		extension.whenGet("/shared").thenReturnText("ok");
		extension.beforeEach(null);
		own.clean();

		extension.afterEach(null);

		assertEquals(200, TestHttp.get(own.url("/shared")).statusCode());
	}

	/** A @Nested class is a scope of its own: what its @BeforeAll stubs must not reach a sibling. */
	@Test
	public void aNestedClassStubIsGoneWhenThatClassEnds() throws Exception {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);
		extension.whenGet("/outer").thenReturnText("ok");
		extension.beforeAll(null);
		extension.whenGet("/nested").thenReturnText("ok");

		extension.afterAll(null);

		assertEquals(200, TestHttp.get(own.url("/outer")).statusCode());
		assertEquals(404, TestHttp.get(own.url("/nested")).statusCode());
	}

	/** Classes sharing one mock run one after the other, and the second must start with nothing. */
	@Test
	public void aClassStubDoesNotReachTheNextClassOnTheSameMock() throws Exception {
		RestMockExtension first = new RestMockExtension(own);
		first.beforeAll(null);
		first.whenGet("/shared").thenReturnText("ok");
		first.afterAll(null);

		new RestMockExtension(own).beforeAll(null);

		assertEquals(404, TestHttp.get(own.url("/shared")).statusCode());
	}

	/** JUnit runs an "after" callback even when the "before" it pairs with was skipped by an earlier failure. */
	@Test
	public void anAfterCallbackWithoutItsBeforeChangesNothing() throws Exception {
		own.startServer();
		own.whenGet("/test").thenReturnText("ok");

		new RestMockExtension(own).afterEach(null);

		assertEquals(200, TestHttp.get(own.url("/test")).statusCode());
	}

	@Test
	public void keepRoutesLeavesATestsStubsAndRequestsForTheNext() throws Exception {
		RestMockExtension extension = new RestMockExtension(own).keepRoutes();
		extension.beforeAll(null);
		extension.beforeEach(null);
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

	/** JUnit fires the class callbacks for a @Nested class too; its end is not the end of the class that owns the server. */
	@Test
	public void aNestedClassEndingLeavesTheServerRunning() {
		RestMockExtension extension = new RestMockExtension(own);
		extension.beforeAll(null);
		int port = own.port();

		extension.beforeAll(null);
		extension.afterAll(null);

		assertEquals(port, own.port());

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
