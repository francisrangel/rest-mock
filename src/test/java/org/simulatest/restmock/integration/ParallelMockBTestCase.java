package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import org.simulatest.restmock.HttpMock;
import org.simulatest.restmock.RestMockExtension;

/**
 * Half of the pair that proves two RestMock-driven classes can run at the same
 * time. Both are CONCURRENT and both own an HttpMock on an OS-assigned port, so
 * neither shares routes, a request log, or a port with the other. On the
 * static-only API this was impossible, and the README said so.
 *
 * Every case here uses its own path, because CONCURRENT applies to the methods
 * of this class as well as to the class itself. The deterministic proof of
 * isolation is IndependentMocksTestCase; what this pair adds is that the seam
 * survives real concurrent execution.
 */
@Execution(ExecutionMode.CONCURRENT)
public class ParallelMockBTestCase {

	private static final HttpClient client = HttpClient.newHttpClient();

	static final HttpMock mock = new HttpMock();

	@RegisterExtension
	static RestMockExtension server = new RestMockExtension(mock, 0).keepRoutes();

	@Test
	public void servesItsOwnRouteWhileTheOtherClassRuns() throws Exception {
		mock.whenGet("/b-one").thenReturnText("b-one");

		assertEquals("b-one", get("/b-one").body());
		assertEquals(1, mock.requests().countForPath("/b-one"));
	}

	@Test
	public void servesASecondRouteOnTheSameMock() throws Exception {
		mock.whenGet("/b-two").thenReturnText("b-two");

		assertEquals("b-two", get("/b-two").body());
		assertEquals(1, mock.requests().countForPath("/b-two"));
	}

	@Test
	public void neverSeesTheOtherClassesRoutes() throws Exception {
		assertTrue(mock.requests().forPath("/a-one").isEmpty(),
			"a request meant for ParallelMockATestCase landed in this log");
		assertEquals(404, get("/a-one").statusCode(),
			"this mock answered a route only ParallelMockATestCase stubbed");
	}

	private HttpResponse<String> get(String path) throws Exception {
		return client.send(
			HttpRequest.newBuilder().uri(URI.create(mock.url(path))).GET().build(),
			HttpResponse.BodyHandlers.ofString());
	}

}
