package org.simulatest.restmock.internal.http;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.utils.BodyFlattener;

final class ParameterExtractor {

	private ParameterExtractor() { }

	/**
	 * Collects everything a response template can reference as {@code ${name}}.
	 *
	 * Sources are applied weakest first, so the resulting precedence is
	 * body fields > query parameters > headers. Path captures are layered on top
	 * by the caller and win over all three.
	 *
	 * Names are matched case-insensitively. That is not cosmetic: the JDK server
	 * rewrites header names to first-letter-uppercase, so a request sent with
	 * {@code X-Tenant} arrives as {@code X-tenant} and an exact-match lookup for
	 * {@code ${X-Tenant}} would silently find nothing.
	 */
	static Map<String, String> extract(URI uri, String body, Map<String, List<String>> headers) {
		Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		appendHeaders(parameters, headers);
		appendQueryParameters(parameters, uri.getRawQuery());
		appendBody(parameters, body, firstHeader(headers, HttpHeader.CONTENT_TYPE));

		return parameters;
	}

	private static void appendBody(Map<String, String> parameters, String body, String contentType) {
		if (ContentType.APPLICATION_FORM_URLENCODED.matches(contentType)) {
			appendQueryParameters(parameters, body);
		} else if (ContentType.APPLICATION_JSON.matches(contentType)) {
			parameters.putAll(BodyFlattener.flattenJson(body));
		} else if (ContentType.TEXT_XML.matches(contentType) || ContentType.APPLICATION_XML.matches(contentType)) {
			parameters.putAll(BodyFlattener.flattenXml(body));
		}
	}

	/** Repeated headers expose their first value. */
	private static void appendHeaders(Map<String, String> parameters, Map<String, List<String>> headers) {
		for (Map.Entry<String, List<String>> header : headers.entrySet()) {
			List<String> values = header.getValue();
			if (values != null && !values.isEmpty()) parameters.put(header.getKey(), values.get(0));
		}
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
