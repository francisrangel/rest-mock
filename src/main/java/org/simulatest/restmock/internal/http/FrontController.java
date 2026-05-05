package org.simulatest.restmock.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.ReceivedRequest;
import org.simulatest.restmock.RequestLog;
import org.simulatest.restmock.internal.response.Binary;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.internal.routing.RouteManager.Match;
import org.simulatest.restmock.internal.utils.LogSafe;

public class FrontController implements HttpHandler {

	private static final Logger log = LoggerFactory.getLogger(FrontController.class);

	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private static final String ALL_METHODS = Arrays.stream(HttpMethod.values())
		.map(Enum::name)
		.collect(Collectors.joining(", "));

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
		HttpMethod httpMethod = HttpMethod.byString(method);
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
			new HashMap<>(exchange.getRequestHeaders()),
			requestBody,
			Instant.now()
		));

		Optional<Match> match = routeManager.lookup(httpMethod, uri.getPath());

		if (match.isEmpty()) {
			log.warn("No route matches {} {} - returning 404", httpMethod, uri.getPath());
			sendStatusOnly(exchange, HttpURLConnection.HTTP_NOT_FOUND);
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

		addHeadersAndAllowCrossDomainAccess(content, exchange);
		exchange.getResponseHeaders().set(HttpHeader.CONTENT_TYPE, content.getContentType().getType());

		if (resolved.route().getMethod() == HttpMethod.OPTIONS) {
			String allow = allowHeaderFor(uri.getPath(), routeManager);
			exchange.getResponseHeaders().set(HttpHeader.ALLOW, allow);
			log.debug("Returning Allow: {} for OPTIONS {}", allow, uri.getPath());
		}

		byte[] body = content instanceof Binary binary
			? binary.getBytes()
			: renderTextBody(content, uri, requestBody, exchange.getRequestHeaders(), resolved.pathCaptures());

		if (resolved.route().getMethod() == HttpMethod.HEAD) {
			exchange.getResponseHeaders().set(HttpHeader.CONTENT_LENGTH, Integer.toString(body.length));
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
			String rendered = content instanceof Binary
				? LogSafe.binaryPlaceholder(body.length)
				: LogSafe.previewBytes(body);
			log.trace("Response body for {} {}: {}", httpMethod, uri.getPath(), rendered);
		}
	}

	private byte[] renderTextBody(Response content, URI uri, String requestBody, Map<String, List<String>> headers, Map<String, String> pathCaptures) {
		Map<String, String> parameters = ParameterExtractor.extract(uri, requestBody, headers);
		parameters.putAll(pathCaptures);
		return replaceParameters(content.getContent(), parameters).getBytes(StandardCharsets.UTF_8);
	}

	private void sendStatusOnly(HttpExchange exchange, int status) throws IOException {
		exchange.sendResponseHeaders(status, -1);
	}

	private String allowHeaderFor(String path, RouteManager routeManager) {
		Set<HttpMethod> methods = routeManager.methodsFor(path);
		methods.add(HttpMethod.OPTIONS);
		return methods.stream().map(Enum::name).collect(Collectors.joining(", "));
	}

	private String replaceParameters(String template, Map<String, String> parameters) {
		return PARAMETER_PATTERN.matcher(template).replaceAll(match -> {
			String key = match.group(1);
			return Matcher.quoteReplacement(parameters.getOrDefault(key, match.group(0)));
		});
	}

	private void addHeadersAndAllowCrossDomainAccess(Response content, HttpExchange exchange) {
		exchange.getResponseHeaders().set(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		exchange.getResponseHeaders().set(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, ALL_METHODS);
		exchange.getResponseHeaders().set(HttpHeader.ACCESS_CONTROL_MAX_AGE, "360");
		exchange.getResponseHeaders().set(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with");
		exchange.getResponseHeaders().set(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

		for (Entry<String, String> header : content.getHeader().entrySet()) {
			exchange.getResponseHeaders().set(header.getKey(), header.getValue());
		}
	}

}
