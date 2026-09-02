package org.simulatest.restmock.internal.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.simulatest.restmock.ReceivedRequest;
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
	 * Bare names are matched exactly, as {@link ReceivedRequest#queryParam} does.
	 * Header names fold case through {@link Placeholders#headerKey}: the JDK
	 * server rewrites {@code X-Tenant} to {@code X-tenant}, so an exact-match
	 * lookup for {@code ${header.X-Tenant}} would silently find nothing.
	 */
	static Map<String, String> extract(ReceivedRequest request) {
		Map<String, String> parameters = new LinkedHashMap<>();

		appendHeaders(parameters, request.headers());
		parameters.putAll(QueryString.parse(request.query()));
		appendBody(parameters, request.body(), request.header(HttpHeader.CONTENT_TYPE).orElse(null));

		return parameters;
	}

	/**
	 * A form body is read like a query string. Pairs are collected first and
	 * copied in as a block, so a form field still overrides a query parameter
	 * of the same name while repeats within one source resolve to the first.
	 */
	private static void appendBody(Map<String, String> parameters, String body, String contentType) {
		if (ContentType.APPLICATION_FORM_URLENCODED.matches(contentType)) {
			parameters.putAll(QueryString.parse(body));
		} else if (ContentType.APPLICATION_JSON.matches(contentType)) {
			parameters.putAll(BodyFlattener.flattenJson(body));
		} else if (ContentType.TEXT_XML.matches(contentType) || ContentType.APPLICATION_XML.matches(contentType)) {
			parameters.putAll(BodyFlattener.flattenXml(body));
		}
	}

	/** Under the header prefix, so an ambient header cannot shadow an authored name. Repeated headers expose their first value. */
	private static void appendHeaders(Map<String, String> parameters, Map<String, List<String>> headers) {
		headers.forEach((name, values) -> {
			if (!values.isEmpty()) parameters.put(Placeholders.headerKey(name), values.get(0));
		});
	}

}
