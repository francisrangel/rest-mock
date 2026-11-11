package org.simulatest.restmock.internal.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.simulatest.restmock.ReceivedRequest;
import org.simulatest.restmock.internal.Placeholders;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.utils.BodyFlattener;

final class ParameterExtractor {

	private static final String NOT_READ = "The request body was not read: ";

	private ParameterExtractor() { }

	/**
	 * Collects everything a response template can reference as {@code ${name}}.
	 *
	 * Sources are applied weakest first, so when a name appears in more than
	 * one the most specific wins: path captures, then body fields, then query
	 * parameters.
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
	static Map<String, String> extract(ReceivedRequest request, Map<String, String> pathCaptures) {
		Map<String, String> parameters = new LinkedHashMap<>();

		appendHeaders(parameters, request.headers());
		parameters.putAll(QueryString.parse(request.query()));
		parameters.putAll(bodyFields(request));
		parameters.putAll(pathCaptures);

		return parameters;
	}

	/**
	 * Why a request that carried a body has no body fields to offer, if that is
	 * so. A placeholder the body was meant to answer then fails for a reason
	 * the list of available names cannot show: the body was never read.
	 */
	static Optional<String> whyBodyGaveNoNames(ReceivedRequest request) {
		if (request.body().isBlank() || !bodyFields(request).isEmpty()) return Optional.empty();

		String contentType = contentType(request);

		if (contentType == null)
			return Optional.of(NOT_READ + "it has no Content-Type.");
		if (!isReadable(contentType))
			return Optional.of(NOT_READ + "its Content-Type is " + contentType + ", not JSON, XML or a form.");

		return Optional.of("The request body gave no names: it is not valid " + contentType + ", or has no fields.");
	}

	/**
	 * A form body is read like a query string. Its pairs arrive as one block,
	 * so a form field still overrides a query parameter of the same name while
	 * repeats within the form resolve to the first.
	 */
	private static Map<String, String> bodyFields(ReceivedRequest request) {
		String contentType = contentType(request);

		if (isForm(contentType)) return QueryString.parse(request.body());
		if (isJson(contentType)) return BodyFlattener.flattenJson(request.body());
		if (isXml(contentType)) return BodyFlattener.flattenXml(request.body());
		return Map.of();
	}

	private static String contentType(ReceivedRequest request) {
		return request.header(HttpHeader.CONTENT_TYPE).orElse(null);
	}

	private static boolean isReadable(String contentType) {
		return isForm(contentType) || isJson(contentType) || isXml(contentType);
	}

	private static boolean isForm(String contentType) {
		return ContentType.APPLICATION_FORM_URLENCODED.matches(contentType);
	}

	private static boolean isJson(String contentType) {
		return ContentType.APPLICATION_JSON.matches(contentType) || ContentType.hasSuffix(contentType, "+json");
	}

	private static boolean isXml(String contentType) {
		return ContentType.TEXT_XML.matches(contentType) || ContentType.APPLICATION_XML.matches(contentType)
			|| ContentType.hasSuffix(contentType, "+xml");
	}

	/** Under the header prefix, so an ambient header cannot shadow an authored name. Repeated headers expose their first value. */
	private static void appendHeaders(Map<String, String> parameters, Map<String, List<String>> headers) {
		headers.forEach((name, values) -> {
			if (!values.isEmpty()) parameters.put(Placeholders.headerKey(name), values.get(0));
		});
	}

}
