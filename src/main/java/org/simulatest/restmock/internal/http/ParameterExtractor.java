package org.simulatest.restmock.internal.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.simulatest.restmock.internal.Placeholders;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.utils.BodyFlattener;

final class ParameterExtractor {

	private ParameterExtractor() { }

	/**
	 * Collects everything a response template can reference as {@code ${name}}.
	 *
	 * Sources are applied weakest first, so the resulting precedence is
	 * body fields > query parameters. Path captures are layered on top by the
	 * caller and win over both.
	 *
	 * Headers sit in their own namespace under {@link Placeholders#HEADER_PREFIX}
	 * and so cannot collide with any of them. See that constant for why they are
	 * not in the bare namespace.
	 *
	 * Bare names are matched exactly, as {@code ReceivedRequest.queryParam} does.
	 * Header names fold case through {@link Placeholders#headerKey}: the JDK
	 * server rewrites {@code X-Tenant} to {@code X-tenant}, so an exact-match
	 * lookup for {@code ${header.X-Tenant}} would silently find nothing.
	 */
	static Map<String, String> extract(URI uri, String body, Map<String, List<String>> headers) {
		Map<String, String> parameters = new LinkedHashMap<>();

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

	/** Under the header prefix, so an ambient header cannot shadow an authored name. Repeated headers expose their first value. */
	private static void appendHeaders(Map<String, String> parameters, Map<String, List<String>> headers) {
		for (Map.Entry<String, List<String>> header : headers.entrySet()) {
			List<String> values = header.getValue();
			if (values != null && !values.isEmpty())
				parameters.put(Placeholders.headerKey(header.getKey()), values.get(0));
		}
	}

	/**
	 * Case-insensitive, so it does not depend on the caller handing us a map that
	 * normalizes on its behalf. {@code com.sun.net.httpserver.Headers} does, which
	 * is the only reason an exact-match lookup ever worked here: the JDK stores a
	 * {@code Content-Type} header under {@code Content-type}, so a plain Map would
	 * miss it and silently skip body flattening, turning every {@code ${field}}
	 * into a 500.
	 */
	private static String firstHeader(Map<String, List<String>> headers, String name) {
		for (Map.Entry<String, List<String>> header : headers.entrySet()) {
			if (!header.getKey().equalsIgnoreCase(name)) continue;

			List<String> values = header.getValue();
			if (values != null && !values.isEmpty()) return values.get(0);
		}

		return null;
	}

	/**
	 * Pairs are collected first and copied in as a block, so a form body still
	 * overrides a query parameter of the same name while repeats within one
	 * source resolve to the first value.
	 */
	private static void appendQueryParameters(Map<String, String> parameters, String raw) {
		parameters.putAll(QueryString.parse(raw));
	}

}
