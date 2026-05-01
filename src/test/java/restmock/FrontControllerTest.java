package restmock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import org.junit.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import restmock.request.FrontController;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.response.TextPlain;

public class FrontControllerTest {

	private final HttpExchange exchange = mock(HttpExchange.class);
	private final RouteManager routeManager = mock(RouteManager.class);
	private final Headers headers = new Headers();

	private void prepare(String method, String uri) {
		when(exchange.getRequestMethod()).thenReturn(method);
		when(exchange.getRequestURI()).thenReturn(URI.create(uri));
		when(exchange.getRequestHeaders()).thenReturn(headers);
		when(exchange.getResponseHeaders()).thenReturn(new Headers());
		when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
	}

	@Test
	public void frontControllerShouldAskRouteManagerForAResponseToProcessARequest() throws IOException {
		prepare("GET", "/test");

		new FrontController().processRequest(exchange, routeManager);

		verify(routeManager).get(any(Route.class));
	}

	@Test
	public void frontControllerShouldReturn404WhenRouteManagerDoesNotKnowARoute() throws IOException {
		prepare("GET", "/test");
		when(routeManager.get(any(Route.class))).thenReturn(null);

		new FrontController().processRequest(exchange, routeManager);

		verify(exchange).sendResponseHeaders(404, -1);
	}

	@Test
	public void frontControllerShouldReturn200WhenRouteManagerKnowsARoute() throws IOException {
		prepare("GET", "/test");
		when(routeManager.get(any(Route.class))).thenReturn(new TextPlain("ok"));

		new FrontController().processRequest(exchange, routeManager);

		long expectedLength = ("ok" + System.lineSeparator()).getBytes().length;
		verify(exchange).sendResponseHeaders(200, expectedLength);
	}

}
