package org.simulatest.restmock.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.ReceivedRequest;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.routing.NoRouteReport;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.internal.routing.RouteManager.Match;

public class FrontController implements HttpHandler {

	private final RouteManager routeManager;
	private final Consumer<ReceivedRequest> recorder;

	public FrontController(RouteManager routeManager, Consumer<ReceivedRequest> recorder) {
		this.routeManager = routeManager;
		this.recorder = recorder;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try (exchange) {
			try {
				processRequest(exchange);
			} catch (RuntimeException failure) {
				respondWithFailure(exchange, failure);
			}
		}
	}

	/**
	 * Without this the exchange is simply closed and the caller sees "unexpected
	 * end of file from server": no status, no message, nothing pointing at the
	 * cause. The mock only ever serves the test that started it, so putting the
	 * failure in the body is the fastest route from symptom to diagnosis.
	 */
	private static void respondWithFailure(HttpExchange exchange, RuntimeException failure) {
		try {
			sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, describe(failure));
		} catch (IOException | RuntimeException tooLate) {
			// Headers are already on the wire, so the status cannot be changed any more.
		}
	}

	private static String describe(RuntimeException failure) {
		return failure.getMessage() == null ? failure.toString() : failure.getMessage();
	}

	private void processRequest(HttpExchange exchange) throws IOException {
		Cors.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());

		String method = exchange.getRequestMethod();
		URI uri = exchange.getRequestURI();

		HttpMethod httpMethod;
		try {
			httpMethod = HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException unsupported) {
			sendText(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED,
				"No support for " + method + ". rest-mock answers " + joinMethodNames(Arrays.asList(HttpMethod.values())) + ".");
			return;
		}

		ReceivedRequest request = new ReceivedRequest(
			httpMethod,
			uri.getPath(),
			uri.getRawQuery(),
			exchange.getRequestHeaders(),
			new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
			Instant.now()
		);

		recorder.accept(request);

		Optional<Match> match = routeManager.lookup(httpMethod, uri.getPath());

		if (match.isPresent()) {
			serve(exchange, request, match.get());
		} else if (httpMethod == HttpMethod.OPTIONS) {
			answerOptions(exchange, uri.getPath());
		} else {
			respondWithNoRoute(exchange, httpMethod, uri.getPath());
		}
	}

	/** Writes the stubbed response for a request that matched. */
	private void serve(HttpExchange exchange, ReceivedRequest request, Match match) throws IOException {
		Response content = match.responder().respond(request);

		if (content.getDelayMillis() > 0) {
			try {
				Thread.sleep(content.getDelayMillis());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Before writeResponseHeaders, so a header the stub set via withHeader
		// still overrides what is derived here.
		if (request.method() == HttpMethod.OPTIONS)
			applyOptionsHeaders(exchange, routeManager.methodsFor(request.path()));

		writeResponseHeaders(content, exchange);

		byte[] body = content.render(ParameterExtractor.extract(request, match.pathCaptures()));

		send(exchange, content.getResponseStatus(), body);
	}

	/**
	 * Answers OPTIONS from the route table. A path with no routes stays a 404 -
	 * but a CORS-decorated one, so a browser reports the 404 instead of an
	 * opaque cross-origin failure.
	 */
	private void answerOptions(HttpExchange exchange, String path) throws IOException {
		Set<HttpMethod> methods = routeManager.methodsFor(path);

		if (methods.isEmpty()) {
			respondWithNoRoute(exchange, HttpMethod.OPTIONS, path);
			return;
		}

		applyOptionsHeaders(exchange, methods);
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
	}

	/**
	 * A 404 that names what is stubbed: the routes are right there in the test,
	 * so the server may as well print them, and "why isn't my mock matching?"
	 * answers itself.
	 */
	private void respondWithNoRoute(HttpExchange exchange, HttpMethod method, String path) throws IOException {
		sendText(exchange, HttpURLConnection.HTTP_NOT_FOUND, NoRouteReport.describe(method, path, routeManager.registeredRoutes()));
	}

	/**
	 * Everything an OPTIONS answer owes the caller: the Allow header, plus the
	 * CORS preflight headers when a browser asked for them.
	 *
	 * Shared by the derived answer and an explicit {@code whenOptions()} stub, so
	 * stubbing a route never switches its preflight off.
	 */
	private static void applyOptionsHeaders(HttpExchange exchange, Set<HttpMethod> methods) {
		String allow = joinMethodNames(methods);
		exchange.getResponseHeaders().set(HttpHeader.ALLOW, allow);

		if (Cors.isPreflight(exchange.getRequestHeaders()))
			Cors.applyPreflight(exchange.getRequestHeaders(), exchange.getResponseHeaders(), allow);
	}

	private static String joinMethodNames(Collection<HttpMethod> methods) {
		return methods.stream().map(Enum::name).collect(Collectors.joining(", "));
	}

	/**
	 * Writes the framework defaults first and the user's own headers last, so a
	 * header set via {@code withHeader} always wins - including {@code Content-Type}.
	 */
	private void writeResponseHeaders(Response content, HttpExchange exchange) {
		Headers responseHeaders = exchange.getResponseHeaders();

		responseHeaders.set(HttpHeader.CONTENT_TYPE, contentTypeHeaderFor(content));
		content.getHeaders().forEach(responseHeaders::set);
	}

	/** Text bodies are encoded UTF-8 and say so; binary bodies carry no known encoding and get the bare type. */
	private static String contentTypeHeaderFor(Response content) {
		ContentType type = content.getContentType();
		return content.isTextual() ? type.utf8() : type.type();
	}

	/** A UTF-8 plain-text answer: the shape of every diagnostic the server writes itself. */
	private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
		exchange.getResponseHeaders().set(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.utf8());
		send(exchange, status, text.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * The one place a status and a body go onto the wire. HEAD carries the
	 * length of the body it would have sent, never the body itself; deciding
	 * that here means no caller can forget it.
	 */
	private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
		if (HttpMethod.HEAD.name().equalsIgnoreCase(exchange.getRequestMethod())) {
			if (allowsContentLength(status)) {
				exchange.getResponseHeaders().set(HttpHeader.CONTENT_LENGTH, Integer.toString(body.length));
			}
			exchange.sendResponseHeaders(status, -1);
			return;
		}

		exchange.sendResponseHeaders(status, body.length);

		try (OutputStream os = exchange.getResponseBody()) {
			os.write(body);
		}
	}

	/** 204 and 304 carry no body; the JDK server rejects the exchange if they declare a length. */
	private static boolean allowsContentLength(int status) {
		return status != HttpURLConnection.HTTP_NO_CONTENT && status != HttpURLConnection.HTTP_NOT_MODIFIED;
	}

}
