package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMock;
import org.simulatest.restmock.RestMock;

/**
 * The reason HttpMock exists: two mocks in one JVM that share nothing.
 *
 * Without an instance seam every route, every recorded request and the port
 * itself were process-wide, so two test classes could not run at once and the
 * README had to warn against it.
 *
 * Deliberately not extending IntegrationTestBase: that base class drives the
 * shared default mock, which is the thing these cases must not depend on.
 */
public class IndependentMocksTestCase {

	private final HttpMock payments = new HttpMock();
	private final HttpMock shipping = new HttpMock();

	@AfterEach
	public void stopBoth() {
		payments.stopServer();
		shipping.stopServer();
	}

	private void startBoth() {
		payments.startServer();
		shipping.startServer();
	}

	@Test
	public void twoMocksBindDifferentPortsAndServeTheirOwnRoutes() throws Exception {
		startBoth();

		payments.whenGet("/status").thenReturnText("payments up");
		shipping.whenGet("/status").thenReturnText("shipping up");

		assertNotEquals(payments.port(), shipping.port(), "each mock needs its own port");
		assertEquals("payments up", get(payments.url("/status")).body());
		assertEquals("shipping up", get(shipping.url("/status")).body());
	}

	@Test
	public void aRouteStubbedOnOneMockIsUnknownToTheOther() throws Exception {
		startBoth();

		payments.whenGet("/charges/1").thenReturnJSON("{\"id\":1}");

		assertEquals(200, get(payments.url("/charges/1")).statusCode());
		assertEquals(404, get(shipping.url("/charges/1")).statusCode());
	}

	@Test
	public void eachMockRecordsOnlyItsOwnRequests() throws Exception {
		startBoth();

		payments.whenGet("/charges/1").thenReturnJSON("{}");
		shipping.whenGet("/labels/1").thenReturnJSON("{}");

		get(payments.url("/charges/1"));
		get(payments.url("/charges/1"));
		get(shipping.url("/labels/1"));

		assertEquals(2, payments.requests().count());
		assertEquals(1, shipping.requests().count());
		assertTrue(payments.requests().forPath("/labels/1").isEmpty(),
			"the payments log picked up a request sent to shipping");
	}

	@Test
	public void cleaningOneMockLeavesTheOtherAlone() throws Exception {
		startBoth();

		payments.whenGet("/status").thenReturnText("payments up");
		shipping.whenGet("/status").thenReturnText("shipping up");

		get(payments.url("/status"));
		payments.clean();

		assertTrue(payments.requests().isEmpty());
		assertEquals(404, get(payments.url("/status")).statusCode());
		assertEquals("shipping up", get(shipping.url("/status")).body(), "shipping lost its route to payments.clean()");
	}

	/**
	 * The static API is a facade over one instance, not a parallel implementation.
	 * A route stubbed through RestMock has to be visible on the default mock, or
	 * the two have drifted apart.
	 */
	@Test
	public void theStaticApiDrivesTheDefaultMock() throws Exception {
		HttpMock defaultMock = RestMock.defaultMock();
		defaultMock.startServer();

		try {
			RestMock.whenGet("/via-static").thenReturnText("ok");

			assertEquals("ok", get(defaultMock.url("/via-static")).body());
			assertEquals(RestMock.port(), defaultMock.port());
			assertEquals(1, defaultMock.requests().count());
			assertEquals(1, RestMock.requests().count());
		} finally {
			RestMock.stopServer();
		}
	}

	@Test
	public void anUnstartedMockHasNoUrlToHandOut() {
		IllegalStateException failure = assertThrows(IllegalStateException.class, payments::baseUrl);

		assertTrue(failure.getMessage().contains("not running"), failure.getMessage());
		assertEquals(-1, payments.port());
	}

	private static HttpResponse<String> get(String url) throws Exception {
		return TestHttp.get(url);
	}

}
