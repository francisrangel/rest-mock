package org.simulatest.restmock.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
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

	private static final String ALL_METHODS = joinMethodNames(Arrays.asList(HttpMethod.values()));

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

		Optional<Match> match = routeManager.lookup(httpMethod, uri.getPath());

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

	private String allowHeaderFor(String path) {
		Set<HttpMethod> methods = routeManager.methodsFor(path);
		methods.add(HttpMethod.OPTIONS);
		return joinMethodNames(methods);
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

		responseHeaders.set(HttpHeader.CONTENT_TYPE, content.getContentType().type());
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, ALL_METHODS);
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_MAX_AGE, "360");
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with");
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

		for (Entry<String, String> header : content.getHeader().entrySet()) {
			responseHeaders.set(header.getKey(), header.getValue());
		}
	}

	/** 204 and 304 carry no body; the JDK server rejects the exchange if they declare a length. */
	private static boolean allowsContentLength(int status) {
		return status != HttpURLConnection.HTTP_NO_CONTENT && status != HttpURLConnection.HTTP_NOT_MODIFIED;
	}

}
