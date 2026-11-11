package org.simulatest.restmock.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
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
			} catch (RuntimeException | Error failure) {
				// Error too: an assertion failing inside a thenAnswer callback is one.
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
	private static void respondWithFailure(HttpExchange exchange, Throwable failure) {
		try {
			sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, describe(failure));
		} catch (IOException | RuntimeException tooLate) {
			// Headers are already on the wire, so the status cannot be changed any more.
		}
	}

	private static String describe(Throwable failure) {
		return failure.getMessage() == null ? failure.toString() : failure.getMessage();
	}

	private void processRequest(HttpExchange exchange) throws IOException {
		Cors.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());

		String method = exchange.getRequestMethod();
		URI uri = exchange.getRequestURI();

		HttpMethod httpMethod;
		try {
			httpMethod = HttpMethod.valueOf(method);
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

		// Routed on the marked path, recorded above under the decoded one.
		String path = EncodedSlash.mark(uri.getRawPath());
		Optional<Match> match = routeManager.lookup(httpMethod, path);

		if (match.isPresent()) {
			// Before the stub's own headers are written, so one set via withHeader still wins.
			if (httpMethod == HttpMethod.OPTIONS) applyOptionsHeaders(exchange, routeManager.methodsFor(path));
			serve(exchange, request, match.get());
		} else if (httpMethod == HttpMethod.OPTIONS) {
			answerOptions(exchange, path);
		} else {
			respondWithNoRoute(exchange, httpMethod, path);
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

		writeResponseHeaders(content, exchange);

		byte[] body = content.render(ParameterExtractor.extract(request, EncodedSlash.restore(match.pathCaptures())));

		send(exchange, content.getResponseStatus(), body);
	}

	/**
	 * Answers OPTIONS from the route table. A path with no routes is a 404,
	 * unless a browser is asking permission: a preflight is always granted, so
	 * the request behind it goes out and comes back with the 404 the page can
	 * read.
	 */
	private void answerOptions(HttpExchange exchange, String path) throws IOException {
		Set<HttpMethod> methods = routeManager.methodsFor(path);

		if (methods.isEmpty() && !Cors.isPreflight(exchange.getRequestHeaders())) {
			respondWithNoRoute(exchange, HttpMethod.OPTIONS, path);
			return;
		}

		applyOptionsHeaders(exchange, methods);
		sendHeadersOnly(exchange, HttpURLConnection.HTTP_NO_CONTENT);
	}

	/**
	 * A 404 that names what is stubbed: the routes are right there in the test,
	 * so the server may as well print them, and "why isn't my mock matching?"
	 * answers itself.
	 */
	private void respondWithNoRoute(HttpExchange exchange, HttpMethod method, String path) throws IOException {
		sendText(exchange, HttpURLConnection.HTTP_NOT_FOUND,
			EncodedSlash.show(NoRouteReport.describe(method, path, routeManager.registeredRoutes())));
	}

	/**
	 * Everything an OPTIONS answer owes the caller: the Allow header, plus the
	 * CORS preflight headers when a browser asked for them.
	 *
	 * Shared by the derived answer and an explicit {@code whenOptions()} stub, so
	 * stubbing a route never switches its preflight off.
	 */
	private static void applyOptionsHeaders(HttpExchange exchange, Set<HttpMethod> methods) {
		if (!methods.isEmpty()) exchange.getResponseHeaders().set(HttpHeader.ALLOW, joinMethodNames(methods));

		if (Cors.isPreflight(exchange.getRequestHeaders()))
			Cors.applyPreflight(exchange.getRequestHeaders(), exchange.getResponseHeaders());
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
		Cors.expose(exchange.getRequestHeaders(), responseHeaders, content.getHeaders().keySet());
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
		if (isHead(exchange)) announceLength(exchange, status, body.length);

		if (hasBodyToWrite(exchange, status, body)) sendWithBody(exchange, status, body);
		else sendHeadersOnly(exchange, status);
	}

	private static boolean hasBodyToWrite(HttpExchange exchange, int status, byte[] body) {
		return !isHead(exchange) && allowsBody(status) && body.length > 0;
	}

	private static boolean isHead(HttpExchange exchange) {
		return HttpMethod.HEAD.name().equals(exchange.getRequestMethod());
	}

	/** 204 and 304 never carry a body. */
	private static boolean allowsBody(int status) {
		return status != HttpURLConnection.HTTP_NO_CONTENT && status != HttpURLConnection.HTTP_NOT_MODIFIED;
	}

	private static void announceLength(HttpExchange exchange, int status, int length) {
		if (allowsBody(status)) exchange.getResponseHeaders().set(HttpHeader.CONTENT_LENGTH, Integer.toString(length));
	}

	/** -1 is the JDK server's "no body": 0 would mean chunked, and any length on a 204 or 304 is logged as a warning. */
	private static void sendHeadersOnly(HttpExchange exchange, int status) throws IOException {
		exchange.sendResponseHeaders(status, -1);
	}

	private static void sendWithBody(HttpExchange exchange, int status, byte[] body) throws IOException {
		exchange.sendResponseHeaders(status, body.length);

		try (OutputStream os = exchange.getResponseBody()) {
			os.write(body);
		}
	}

}
