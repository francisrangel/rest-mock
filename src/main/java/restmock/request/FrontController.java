package restmock.request;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import restmock.response.Response;
import restmock.response.visitor.ReplacerParametersVisitor;

public class FrontController implements HttpHandler {

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			processRequest(exchange, RouteManager.getInstance());
		} finally {
			exchange.close();
		}
	}

	public void processRequest(HttpExchange exchange, RouteManager routeManager) throws IOException {
		String method = exchange.getRequestMethod();
		URI uri = exchange.getRequestURI();
		Route route = new Route(method, uri.getPath());
		Response content = routeManager.get(route);

		if (content == null) {
			sendStatusOnly(exchange, HttpURLConnection.HTTP_NOT_FOUND);
			return;
		}

		Map<String, String> parameters = parseParameters(exchange, uri);
		new ReplacerParametersVisitor(parameters).visit(content);

		addHeadersAndAllowCrossDomainAccess(content, exchange);
		exchange.getResponseHeaders().set("Content-Type", content.getContentType().getType());

		byte[] body = (content.getContent() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(content.getResponseStatus(), body.length);

		try (OutputStream os = exchange.getResponseBody()) {
			os.write(body);
		}
	}

	private void sendStatusOnly(HttpExchange exchange, int status) throws IOException {
		exchange.sendResponseHeaders(status, -1);
	}

	private Map<String, String> parseParameters(HttpExchange exchange, URI uri) throws IOException {
		Map<String, String> parameters = new HashMap<>();
		appendQueryParameters(parameters, uri.getRawQuery());

		String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
		if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			appendQueryParameters(parameters, body);
		}

		return parameters;
	}

	private void appendQueryParameters(Map<String, String> parameters, String raw) {
		if (raw == null || raw.isEmpty()) return;

		for (String pair : raw.split("&")) {
			int eq = pair.indexOf('=');
			if (eq < 0) continue;

			String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
			String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
			parameters.put(key, value);
		}
	}

	private void addHeadersAndAllowCrossDomainAccess(Response content, HttpExchange exchange) {
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
		exchange.getResponseHeaders().set("Access-Control-Max-Age", "360");
		exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "x-requested-with");
		exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

		for (Entry<String, String> header : content.getHeader().entrySet()) {
			exchange.getResponseHeaders().set(header.getKey(), header.getValue());
		}
	}

}
