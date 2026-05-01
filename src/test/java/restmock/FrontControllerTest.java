package restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import restmock.http.FrontController;
import restmock.http.HttpMethod;
import restmock.routing.Route;
import restmock.routing.RouteManager;
import restmock.response.TextPlain;

public class FrontControllerTest {

	private final HttpExchange exchange = mock(HttpExchange.class);
	private final RouteManager routeManager = mock(RouteManager.class);
	private final RequestLog requestLog = new RequestLog();
	private final Headers headers = new Headers();

	private void prepare(String method, String uri) {
		when(exchange.getRequestMethod()).thenReturn(method);
		when(exchange.getRequestURI()).thenReturn(URI.create(uri));
		when(exchange.getRequestHeaders()).thenReturn(headers);
		when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(exchange.getResponseHeaders()).thenReturn(new Headers());
		when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
	}

	@Test
	public void frontControllerShouldAskRouteManagerForAResponseToProcessARequest() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");
		when(routeManager.lookup(any(HttpMethod.class), any(String.class))).thenReturn(Optional.empty());

		new FrontController(routeManager, requestLog).processRequest(exchange);

		verify(routeManager).lookup(HttpMethod.GET, "/test");
	}

	@Test
	public void frontControllerShouldReturn404WhenRouteManagerDoesNotKnowARoute() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");
		when(routeManager.lookup(any(HttpMethod.class), any(String.class))).thenReturn(Optional.empty());

		new FrontController(routeManager, requestLog).processRequest(exchange);

		verify(exchange).sendResponseHeaders(404, -1);
	}

	@Test
	public void frontControllerShouldReturn200WhenRouteManagerKnowsARoute() throws IOException {
		prepare(HttpMethod.GET.name(), "/test");
		Route route = new Route(HttpMethod.GET, "/test");
		RouteManager.Match match = new RouteManager.Match(route, new TextPlain("ok"), new HashMap<>());
		when(routeManager.lookup(any(HttpMethod.class), any(String.class))).thenReturn(Optional.of(match));

		new FrontController(routeManager, requestLog).processRequest(exchange);

		long expectedLength = ("ok" + System.lineSeparator()).getBytes().length;
		verify(exchange).sendResponseHeaders(200, expectedLength);
	}

	@Test
	public void requestIsCaptured() throws IOException {
		prepare(HttpMethod.GET.name(), "/test?q=1");
		when(routeManager.lookup(any(HttpMethod.class), any(String.class))).thenReturn(Optional.empty());

		new FrontController(routeManager, requestLog).processRequest(exchange);

		assertEquals(1, requestLog.count());
		assertEquals(HttpMethod.GET, requestLog.last().orElseThrow().method());
		assertEquals("/test", requestLog.last().orElseThrow().path());
		assertEquals("q=1", requestLog.last().orElseThrow().query());
	}

}
