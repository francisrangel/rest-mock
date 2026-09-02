package org.simulatest.restmock.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.simulatest.restmock.internal.utils.LogSafe;

public class FrontController implements HttpHandler {

	private static final Logger log = LoggerFactory.getLogger(FrontController.class);

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
	 * Without this the exchange was simply closed and the caller saw "unexpected
	 * end of file from server" - no status, no message, nothing pointing at the
	 * cause. The mock only ever serves the test that started it, so putting the
	 * failure in the body is the fastest route from symptom to diagnosis.
	 */
	private void respondWithFailure(HttpExchange exchange, RuntimeException failure) {
		log.error("Failed to handle {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), failure);

		try {
			sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, describe(failure));
		} catch (IOException | RuntimeException tooLate) {
			// Headers are already on the wire, so the status cannot be changed
			// any more. The log above is the only record left.
			log.debug("Could not send the 500 for {}", exchange.getRequestURI(), tooLate);
		}
	}

	private static String describe(RuntimeException failure) {
		return failure.getMessage() == null ? failure.toString() : failure.getMessage();
	}

	public void processRequest(HttpExchange exchange) throws IOException {
		Cors.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());

		String method = exchange.getRequestMethod();
		URI uri = exchange.getRequestURI();

		HttpMethod httpMethod;
		try {
			httpMethod = HttpMethod.byString(method);
		} catch (IllegalArgumentException unsupported) {
			log.warn("Unsupported HTTP method {} for {} - returning 501", method, uri.getPath());
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

		if (log.isTraceEnabled()) {
			log.trace("Received {} {} headers={} body={}",
				httpMethod, uri.getPath(), LogSafe.maskHeaders(request.headers()), LogSafe.truncate(request.body()));
		}

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
		Response content = match.response();

		if (content.getDelayMillis() > 0) {
			try {
				Thread.sleep(content.getDelayMillis());
			} catch (InterruptedException e) {
				log.warn("Delay interrupted for {} {}", request.method(), request.path());
				Thread.currentThread().interrupt();
			}
		}

		// Before writeResponseHeaders, so a header the stub set via withHeader
		// still overrides what is derived here.
		if (request.method() == HttpMethod.OPTIONS)
			applyOptionsHeaders(exchange, request.path(), routeManager.methodsFor(request.path()));

		writeResponseHeaders(content, exchange);

		// A body with nothing to substitute never reads the parameters, so a
		// static stub does not pay to parse the request body on every call.
		Map<String, String> parameters = content.usesParameters()
			? ParameterExtractor.extract(request, match.pathCaptures())
			: Map.of();
		byte[] body = content.render(parameters);

		send(exchange, content.getResponseStatus(), body);

		boolean head = request.method() == HttpMethod.HEAD;
		log.debug("{} {} -> {} ({} bytes{})", request.method(), request.path(), content.getResponseStatus(), body.length,
			head ? ", no body" : "");
		if (log.isTraceEnabled() && !head) {
			String rendered = content.isTextual()
				? LogSafe.truncate(new String(body, StandardCharsets.UTF_8))
				: "<" + body.length + " bytes binary>";
			log.trace("Response body for {} {}: {}", request.method(), request.path(), rendered);
		}
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

		applyOptionsHeaders(exchange, path, methods);
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
	}

	/**
	 * A 404 that names what is stubbed. The empty body this used to send made
	 * "why isn't my mock matching?" a debugging session; the routes are right
	 * there in the test, so the server may as well print them.
	 */
	private void respondWithNoRoute(HttpExchange exchange, HttpMethod method, String path) throws IOException {
		String report = NoRouteReport.describe(method, path, routeManager.registeredRoutes());

		log.warn("No route matches {} {} - returning 404", method, path);
		log.debug("{}", report);

		sendText(exchange, HttpURLConnection.HTTP_NOT_FOUND, report);
	}

	/**
	 * Everything an OPTIONS answer owes the caller: the Allow header, plus the
	 * CORS preflight headers when a browser asked for them.
	 *
	 * Shared by the derived answer and an explicit {@code whenOptions()} stub.
	 * The stub used to skip this, so stubbing a route switched its preflight
	 * off - the browser got a body but no Access-Control-Allow-Methods, and CORS
	 * that worked before the stub stopped working after it.
	 */
	private void applyOptionsHeaders(HttpExchange exchange, String path, Set<HttpMethod> methods) {
		String allow = joinMethodNames(methods);
		exchange.getResponseHeaders().set(HttpHeader.ALLOW, allow);

		if (Cors.isPreflight(exchange.getRequestHeaders()))
			Cors.applyPreflight(exchange.getRequestHeaders(), exchange.getResponseHeaders(), allow);

		log.debug("OPTIONS {} -> Allow: {}", path, allow);
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
