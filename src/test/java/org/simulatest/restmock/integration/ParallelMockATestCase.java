package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class ParallelMockATestCase {

	@RegisterExtension
	static RestMockExtension restMock = new RestMockExtension(new HttpMock()).keepRoutes();

	@Test
	public void servesItsOwnRouteWhileTheOtherClassRuns() throws Exception {
		restMock.whenGet("/a-one").thenReturnText("a-one");

		assertEquals("a-one", get("/a-one").body());
		assertEquals(1, restMock.requests().countForPath("/a-one"));
	}

	@Test
	public void servesASecondRouteOnTheSameMock() throws Exception {
		restMock.whenGet("/a-two").thenReturnText("a-two");

		assertEquals("a-two", get("/a-two").body());
		assertEquals(1, restMock.requests().countForPath("/a-two"));
	}

	@Test
	public void neverSeesTheOtherClassesRoutes() throws Exception {
		assertTrue(restMock.requests().forPath("/b-one").isEmpty(),
			"a request meant for ParallelMockBTestCase landed in this log");
		assertEquals(404, get("/b-one").statusCode(),
			"this mock answered a route only ParallelMockBTestCase stubbed");
	}

	private HttpResponse<String> get(String path) throws Exception {
		return TestHttp.get(restMock.url(path));
	}

}
