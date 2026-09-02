package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import org.simulatest.restmock.internal.http.FrontController;
import org.simulatest.restmock.internal.response.TextPlain;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;

/** Drives the handler with a hand-rolled exchange, so no socket and no mocking library are involved. */
public class FrontControllerTest {

	private final RouteManager routeManager = new RouteManager();
	private final RequestLog requestLog = new RequestLog();
	private final FrontController controller = new FrontController(routeManager, requestLog::add);

	private void stub(HttpMethod method, String uri, String body) {
		routeManager.registerRoute(new Route(method, uri), new TextPlain(body));
	}

	@Test
	public void aStubbedRouteIsServedWithItsBody() throws IOException {
		stub(HttpMethod.GET, "/test", "ok");
		FakeExchange exchange = new FakeExchange("GET", "/test");

		controller.processRequest(exchange);

		assertEquals(200, exchange.status);
		assertEquals("ok".length(), exchange.declaredLength);
		assertEquals("ok", exchange.body());
	}

	@Test
	public void anUnstubbedRouteIs404WithABody() throws IOException {
		stub(HttpMethod.GET, "/known", "ok");
		FakeExchange exchange = new FakeExchange("GET", "/test");

		controller.processRequest(exchange);

		assertEquals(404, exchange.status);
		assertTrue(exchange.declaredLength > 0, "the 404 must carry a body");
	}

	/** An empty 404 body left "why isn't my mock matching?" to guesswork. */
	@Test
	public void theNotFoundBodySaysWhatIsStubbed() throws IOException {
		stub(HttpMethod.GET, "/known", "ok");
		FakeExchange exchange = new FakeExchange("GET", "/test");

		controller.processRequest(exchange);

		String body = exchange.body();
		assertTrue(body.contains("No stub for GET /test"), body);
		assertTrue(body.contains("GET     /known"), body);
	}

	@Test
	public void requestIsCaptured() throws IOException {
		FakeExchange exchange = new FakeExchange("POST", "/test?q=1");
		exchange.requestHeaders.add("Content-Type", "application/json");
		exchange.requestBody = "{\"name\":\"Bob\"}";

		controller.processRequest(exchange);

		assertEquals(1, requestLog.count());
		ReceivedRequest captured = requestLog.last().orElseThrow();
		assertEquals(HttpMethod.POST, captured.method());
		assertEquals("/test", captured.path());
		assertEquals("q=1", captured.query());
		assertEquals("{\"name\":\"Bob\"}", captured.body());
		assertEquals("application/json", captured.headers().get("Content-type").get(0));
	}

	@Test
	public void anUnsupportedHttpMethodIsRejectedWithNotImplemented() throws IOException {
		FakeExchange exchange = new FakeExchange("TRACE", "/test");

		controller.processRequest(exchange);

		assertEquals(501, exchange.status);
		assertTrue(exchange.body().startsWith("No support for TRACE."), exchange.body());
		assertTrue(requestLog.isEmpty(), "an unsupported method must not be recorded");
	}

	/**
	 * A handler that blew up used to close the exchange silently, so the caller
	 * saw "unexpected end of file from server" with nothing to go on.
	 */
	@Test
	public void aFailureWhileRenderingBecomesA500CarryingTheReason() throws IOException {
		stub(HttpMethod.GET, "/boom", "${nope}");
		FakeExchange exchange = new FakeExchange("GET", "/boom");

		controller.handle(exchange);

		assertEquals(500, exchange.status);
		assertEquals("No value for ${nope}. Available names: (none)", exchange.body());
		assertTrue(exchange.closed, "handle() owns the exchange and must close it");
	}

	/**
	 * Just enough of HttpExchange to feed the handler a request and read back
	 * what it sent. Everything the handler does not touch throws, so a new
	 * dependency on the exchange shows up as a test failure rather than a null.
	 */
	private static final class FakeExchange extends HttpExchange {

		final Headers requestHeaders = new Headers();
		final Headers responseHeaders = new Headers();
		final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
		final String method;
		final URI uri;

		String requestBody = "";
		int status;
		long declaredLength;
		boolean closed;

		FakeExchange(String method, String uri) {
			this.method = method;
			this.uri = URI.create(uri);
		}

		String body() {
			return responseBody.toString(StandardCharsets.UTF_8);
		}

		@Override public Headers getRequestHeaders() { return requestHeaders; }
		@Override public Headers getResponseHeaders() { return responseHeaders; }
		@Override public URI getRequestURI() { return uri; }
		@Override public String getRequestMethod() { return method; }
		@Override public InputStream getRequestBody() { return new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)); }
		@Override public OutputStream getResponseBody() { return responseBody; }
		@Override public int getResponseCode() { return status; }
		@Override public void close() { closed = true; }

		@Override
		public void sendResponseHeaders(int status, long length) {
			this.status = status;
			this.declaredLength = length;
		}

		@Override public HttpContext getHttpContext() { throw new UnsupportedOperationException(); }
		@Override public InetSocketAddress getRemoteAddress() { throw new UnsupportedOperationException(); }
		@Override public InetSocketAddress getLocalAddress() { throw new UnsupportedOperationException(); }
		@Override public String getProtocol() { throw new UnsupportedOperationException(); }
		@Override public Object getAttribute(String name) { throw new UnsupportedOperationException(); }
		@Override public void setAttribute(String name, Object value) { throw new UnsupportedOperationException(); }
		@Override public void setStreams(InputStream in, OutputStream out) { throw new UnsupportedOperationException(); }
		@Override public HttpPrincipal getPrincipal() { throw new UnsupportedOperationException(); }

	}

}
