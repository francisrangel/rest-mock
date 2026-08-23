package org.simulatest.restmock.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.ReceivedRequest;
import org.simulatest.restmock.RequestLog;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.internal.routing.RouteManager.Match;
import org.simulatest.restmock.internal.utils.LogSafe;

public class FrontController implements HttpHandler {

	private static final Logger log = LoggerFactory.getLogger(FrontController.class);

	private final RouteManager routeManager;
	private final RequestLog requestLog;

	public FrontController(RouteManager routeManager, RequestLog requestLog) {
		this.routeManager = routeManager;
		this.requestLog = requestLog;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try (exchange) {
			processRequest(exchange);
		}
	}

	public void processRequest(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		URI uri = exchange.getRequestURI();

		HttpMethod httpMethod;
		try {
			httpMethod = HttpMethod.byString(method);
		} catch (IllegalArgumentException unsupported) {
			log.warn("Unsupported HTTP method {} for {} - returning 501", method, uri.getPath());
			Cors.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_IMPLEMENTED, -1);
			return;
		}

		String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

		if (log.isTraceEnabled()) {
			log.trace("Received {} {} headers={} body={}",
				httpMethod, uri.getPath(),
				LogSafe.maskHeaders(exchange.getRequestHeaders()),
				LogSafe.truncate(requestBody));
		}

		requestLog.add(new ReceivedRequest(
			httpMethod,
			uri.getPath(),
			uri.getRawQuery(),
			exchange.getRequestHeaders(),
			requestBody,
			Instant.now()
		));

		Cors.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());

		Optional<Match> match = routeManager.lookup(httpMethod, uri.getPath());

		// An explicit whenHead()/whenOptions() stub wins. Failing that, both are
		// derived from what is registered: HEAD from the GET route, OPTIONS from
		// every method the path answers.
		if (match.isEmpty() && httpMethod == HttpMethod.HEAD) {
			match = routeManager.lookup(HttpMethod.GET, uri.getPath());
		}

		if (match.isEmpty() && httpMethod == HttpMethod.OPTIONS) {
			answerOptions(exchange, uri.getPath());
			return;
		}

		if (match.isEmpty()) {
			log.warn("No route matches {} {} - returning 404", httpMethod, uri.getPath());
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
			return;
		}

		Match resolved = match.get();
		Response content = resolved.response();

		if (content.getDelayMillis() > 0) {
			try {
				Thread.sleep(content.getDelayMillis());
			} catch (InterruptedException e) {
				log.warn("Delay interrupted for {} {}", httpMethod, uri.getPath());
				Thread.currentThread().interrupt();
			}
		}

		writeResponseHeaders(content, exchange);

		if (httpMethod == HttpMethod.OPTIONS) {
			String allow = allowHeaderFor(uri.getPath());
			exchange.getResponseHeaders().set(HttpHeader.ALLOW, allow);
			log.debug("Returning Allow: {} for OPTIONS {}", allow, uri.getPath());
		}

		byte[] body = content.render(
			parametersFor(uri, requestBody, exchange.getRequestHeaders(), resolved.pathCaptures()));

		if (httpMethod == HttpMethod.HEAD) {
			if (allowsContentLength(content.getResponseStatus())) {
				exchange.getResponseHeaders().set(HttpHeader.CONTENT_LENGTH, Integer.toString(body.length));
			}
			exchange.sendResponseHeaders(content.getResponseStatus(), -1);
			log.debug("HEAD {} -> {} (Content-Length {}, no body)", uri.getPath(), content.getResponseStatus(), body.length);
			return;
		}

		exchange.sendResponseHeaders(content.getResponseStatus(), body.length);

		try (OutputStream os = exchange.getResponseBody()) {
			os.write(body);
		}

		log.debug("{} {} -> {} ({} bytes)", httpMethod, uri.getPath(), content.getResponseStatus(), body.length);
		if (log.isTraceEnabled()) {
			String rendered = content.isTextual()
				? LogSafe.previewBytes(body)
				: LogSafe.binaryPlaceholder(body.length);
			log.trace("Response body for {} {}: {}", httpMethod, uri.getPath(), rendered);
		}
	}

	private static Map<String, String> parametersFor(URI uri, String requestBody, Map<String, List<String>> headers, Map<String, String> pathCaptures) {
		Map<String, String> parameters = ParameterExtractor.extract(uri, requestBody, headers);
		parameters.putAll(pathCaptures);
		return parameters;
	}

	/**
	 * Answers OPTIONS from the route table. A path with no routes stays a 404 -
	 * but a CORS-decorated one, so a browser reports the 404 instead of an
	 * opaque cross-origin failure.
	 */
	private void answerOptions(HttpExchange exchange, String path) throws IOException {
		Set<HttpMethod> methods = advertisedMethods(path);

		if (methods.isEmpty()) {
			log.warn("OPTIONS {} but no route is registered for that path - returning 404", path);
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
			return;
		}

		String allow = joinMethodNames(methods);

		if (Cors.isPreflight(HttpMethod.OPTIONS, exchange.getRequestHeaders())) {
			Cors.applyPreflight(exchange.getRequestHeaders(), exchange.getResponseHeaders(), allow);
		}

		exchange.getResponseHeaders().set(HttpHeader.ALLOW, allow);
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);

		log.debug("OPTIONS {} -> 204 (Allow: {})", path, allow);
	}

	private String allowHeaderFor(String path) {
		return joinMethodNames(advertisedMethods(path));
	}

	/**
	 * What the path actually answers, which is more than what was registered:
	 * OPTIONS is always served, and HEAD is served wherever GET is.
	 */
	private Set<HttpMethod> advertisedMethods(String path) {
		Set<HttpMethod> methods = routeManager.methodsFor(path);
		if (methods.isEmpty()) return methods;

		if (methods.contains(HttpMethod.GET)) methods.add(HttpMethod.HEAD);
		methods.add(HttpMethod.OPTIONS);
		return methods;
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

		for (Entry<String, String> header : content.getHeader().entrySet()) {
			responseHeaders.set(header.getKey(), header.getValue());
		}
	}

	/**
	 * Text bodies are encoded UTF-8, so the header has to say so. Without it a
	 * client applying the historical text/* default decodes "cafe" with an
	 * accent as mojibake. Binary bodies are passed through verbatim and carry no
	 * known encoding, so they get the bare type.
	 */
	private static String contentTypeHeaderFor(Response content) {
		String type = content.getContentType().type();
		return content.isTextual() ? type + "; charset=utf-8" : type;
	}

	/** 204 and 304 carry no body; the JDK server rejects the exchange if they declare a length. */
	private static boolean allowsContentLength(int status) {
		return status != HttpURLConnection.HTTP_NO_CONTENT && status != HttpURLConnection.HTTP_NOT_MODIFIED;
	}

}
