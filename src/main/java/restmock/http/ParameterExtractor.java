package restmock.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import restmock.response.ContentType;
import restmock.utils.JsonFlattener;

final class ParameterExtractor {

	private ParameterExtractor() { }

	static Map<String, String> extract(HttpExchange exchange, URI uri) throws IOException {
		Map<String, String> parameters = new HashMap<>();
		appendQueryParameters(parameters, uri.getRawQuery());

		String contentType = exchange.getRequestHeaders().getFirst(HttpHeader.CONTENT_TYPE);
		if (contentType == null) return parameters;

		String lower = contentType.toLowerCase();
		if (lower.contains(ContentType.APPLICATION_FORM_URLENCODED.getType())) {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			appendQueryParameters(parameters, body);
		} else if (lower.contains(ContentType.APPLICATION_JSON.getType())) {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			parameters.putAll(JsonFlattener.flatten(body));
		}

		return parameters;
	}

	private static void appendQueryParameters(Map<String, String> parameters, String raw) {
		if (raw == null || raw.isEmpty()) return;

		for (String pair : raw.split("&")) {
			int eq = pair.indexOf('=');
			if (eq < 0) continue;

			String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
			String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
			parameters.put(key, value);
		}
	}

}
