package restmock.http;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import restmock.response.ContentType;
import restmock.utils.JsonFlattener;

final class ParameterExtractor {

	private ParameterExtractor() { }

	static Map<String, String> extract(URI uri, String body, Map<String, List<String>> headers) {
		Map<String, String> parameters = new HashMap<>();
		appendQueryParameters(parameters, uri.getRawQuery());

		String contentType = firstHeader(headers, HttpHeader.CONTENT_TYPE);
		if (contentType == null) return parameters;

		String lower = contentType.toLowerCase();
		if (lower.contains(ContentType.APPLICATION_FORM_URLENCODED.getType())) {
			appendQueryParameters(parameters, body);
		} else if (lower.contains(ContentType.APPLICATION_JSON.getType())) {
			parameters.putAll(JsonFlattener.flatten(body));
		}

		return parameters;
	}

	private static String firstHeader(Map<String, List<String>> headers, String name) {
		List<String> values = headers.get(name);
		return (values != null && !values.isEmpty()) ? values.get(0) : null;
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
