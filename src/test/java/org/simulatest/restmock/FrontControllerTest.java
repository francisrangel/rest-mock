package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import org.simulatest.restmock.internal.http.FrontController;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.response.TextPlain;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;

public class FrontControllerTest {

	private final HttpExchange exchange = mock(HttpExchange.class);
	private final RouteManager routeManager = mock(RouteManager.class);
	private final RequestLog requestLog = new RequestLog();
	private final Headers headers = new Headers();
	private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
	private final FrontController controller = new FrontController(routeManager, requestLog::add);

	private void prepare(String method, String uri) {
		when(routeManager.lookup(any(HttpMethod.class), any(String.class))).thenReturn(Optional.empty());
		when(routeManager.registeredRoutes()).thenReturn(List.of(new Route(HttpMethod.GET, "/known")));
		when(exchange.getRequestMethod()).thenReturn(method);
		when(exchange.getRequestURI()).thenReturn(URI.create(uri));
		when(exchange.getRequestHeaders()).thenReturn(headers);
		when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(exchange.getResponseHeaders()).thenReturn(new Headers());
		when(exchange.getResponseBody()).thenReturn(responseBody);
	}

	@Test
	public void frontControllerShouldAskRouteManagerForAResponseToProcessARequest() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");

		controller.processRequest(exchange);

		verify(routeManager).lookup(HttpMethod.GET, "/test");
	}

	@Test
	public void frontControllerShouldReturn404WhenRouteManagerDoesNotKnowARoute() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");

		controller.processRequest(exchange);

		verify(exchange).sendResponseHeaders(eq(404), longThat(length -> length > 0));
	}

	/** An empty 404 body left "why isn't my mock matching?" to guesswork. */
	@Test
	public void theNotFoundBodySaysWhatIsStubbed() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");

		controller.processRequest(exchange);

		String body = responseBody.toString(StandardCharsets.UTF_8);
		assertTrue(body.contains("No stub for GET /test"), body);
		assertTrue(body.contains("GET     /known"), body);
	}

	@Test
	public void frontControllerShouldReturn200WhenRouteManagerKnowsARoute() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");
		Route route = new Route(HttpMethod.GET, "/test");
		RouteManager.Match match = new RouteManager.Match(route, new TextPlain("ok"), new HashMap<>());
		when(routeManager.lookup(any(HttpMethod.class), any(String.class))).thenReturn(Optional.of(match));

		controller.processRequest(exchange);

		verify(exchange).sendResponseHeaders(200, "ok".getBytes().length);
	}

	@Test
	public void requestIsCaptured() throws IOException {
		prepare(HttpMethod.POST.name(), "/test?q=1");
		headers.add("Content-Type", "application/json");
		when(exchange.getRequestBody()).thenReturn(
			new ByteArrayInputStream("{\"name\":\"Bob\"}".getBytes(StandardCharsets.UTF_8)));

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
		prepare("TRACE", "/test");

		controller.processRequest(exchange);

		verify(exchange).sendResponseHeaders(501, -1);
		assertTrue(requestLog.isEmpty(), "an unsupported method must not be recorded");
	}

	/**
	 * A handler that blew up used to close the exchange silently, so the caller
	 * saw "unexpected end of file from server" with nothing to go on.
	 */
	@Test
	public void aFailureWhileRenderingBecomesA500CarryingTheReason() throws IOException {
		prepare(HttpMethod.GET.name(), "/boom");

		Response exploding = mock(Response.class);
		when(exploding.getContentType()).thenReturn(ContentType.TEXT_PLAIN);
		when(exploding.getResponseStatus()).thenReturn(200);
		when(exploding.render(any())).thenThrow(new IllegalStateException("no value for ${nope}"));
		when(routeManager.lookup(HttpMethod.GET, "/boom"))
			.thenReturn(Optional.of(new RouteManager.Match(new Route(HttpMethod.GET, "/boom"), exploding, Map.of())));

		ByteArrayOutputStream body = new ByteArrayOutputStream();
		when(exchange.getResponseBody()).thenReturn(body);

		controller.handle(exchange);

		verify(exchange).sendResponseHeaders(500, "no value for ${nope}".length());
		assertEquals("no value for ${nope}", body.toString(StandardCharsets.UTF_8));
	}

}