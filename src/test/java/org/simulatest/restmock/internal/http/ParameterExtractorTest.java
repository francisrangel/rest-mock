package org.simulatest.restmock.internal.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.internal.response.ContentType;

public class ParameterExtractorTest {

	private Map<String, List<String>> headers = new HashMap<>();

	private void withContentType(String contentType) {
		headers.put(HttpHeader.CONTENT_TYPE, List.of(contentType));
	}

	@Test
	public void noQueryNoBody() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "", headers);

		assertTrue(params.isEmpty());
	}

	@Test
	public void singleQueryParameter() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?name=Bob"), "", headers);

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void multipleQueryParameters() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?a=1&b=2&c=3"), "", headers);

		assertEquals("1", params.get("a"));
		assertEquals("2", params.get("b"));
		assertEquals("3", params.get("c"));
	}

	@Test
	public void urlEncodedQueryParameter() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?msg=hello%20world"), "", headers);

		assertEquals("hello world", params.get("msg"));
	}

	@Test
	public void queryParameterWithoutValueIsSkipped() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?flag&name=Bob"), "", headers);

		assertEquals(1, params.size());
		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void formEncodedBody() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.type() + "; charset=UTF-8");

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "name=Bob&age=25", headers);

		assertEquals("Bob", params.get("name"));
		assertEquals("25", params.get("age"));
	}

	@Test
	public void jsonBody() {
		withContentType(ContentType.APPLICATION_JSON.type());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "{\"user\":{\"name\":\"Bob\"}}", headers);

		assertEquals("Bob", params.get("user.name"));
	}

	@Test
	public void unknownContentTypeIgnoresBody() {
		withContentType(ContentType.TEXT_PLAIN.type());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "name=Bob", headers);

		assertNull(params.get("name"), "a text/plain body must not be parsed for parameters");
		// the request's own headers are parameters too, so the map is not empty
		assertEquals(ContentType.TEXT_PLAIN.type(), params.get(HttpHeader.CONTENT_TYPE));
	}

	@Test
	public void queryAndFormBodyMerge() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.type());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?from=query"), "from_body=yes", headers);

		assertEquals("query", params.get("from"));
		assertEquals("yes", params.get("from_body"));
	}

	@Test
	public void formBodyOverridesQueryWhenSameKey() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.type());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?name=Alice"), "name=Bob", headers);

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void trailingAmpersandInQuery() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?name=Bob&"), "", headers);

		assertEquals("Bob", params.get("name"));
		assertEquals(1, params.size());
	}

	@Test
	public void valueContainingEqualsSign() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?expr=a%3Db"), "", headers);

		assertEquals("a=b", params.get("expr"));
	}

	@Test
	public void contentTypeCaseInsensitive() {
		withContentType("APPLICATION/JSON");

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "{\"name\":\"Bob\"}", headers);

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void emptyQueryString() {
		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?"), "", headers);

		assertTrue(params.isEmpty());
	}

	// --- documented in RestMockResponse: "${name} may reference query parameters,
	// --- headers, JSON/XML body fields, and path captures"

	@Test
	public void headerValuesAreExposedAsParameters() {
		headers.put("X-Tenant", List.of("acme"));

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "", headers);

		assertEquals("acme", params.get("X-Tenant"));
	}

	@Test
	public void onlyTheFirstValueOfARepeatedHeaderIsExposed() {
		headers.put("X-Tenant", List.of("acme", "other"));

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "", headers);

		assertEquals("acme", params.get("X-Tenant"));
	}

	@Test
	public void headerNamesAreMatchedIgnoringCase() {
		// the JDK server rewrites X-Tenant to X-tenant, so a template that spells the
		// header the way the client sent it still has to resolve
		headers.put("X-tenant", List.of("acme"));

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "", headers);

		assertEquals("acme", params.get("X-Tenant"));
		assertEquals("acme", params.get("x-tenant"));
	}

	@Test
	public void queryParametersWinOverHeadersWithTheSameName() {
		headers.put("name", List.of("from-header"));

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?name=from-query"), "", headers);

		assertEquals("from-query", params.get("name"));
	}

	@Test
	public void bodyFieldsWinOverHeadersWithTheSameName() {
		withContentType(ContentType.APPLICATION_JSON.type());
		headers.put("name", List.of("from-header"));

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "{\"name\":\"from-body\"}", headers);

		assertEquals("from-body", params.get("name"));
	}

	@Test
	public void xmlBodyFieldsAreExposedAsParameters() {
		withContentType(ContentType.TEXT_XML.type());

		Map<String, String> params = ParameterExtractor.extract(
			URI.create("/test"), "<developer><name>Bob</name><age>25</age></developer>", headers);

		assertEquals("Bob", params.get("name"));
		assertEquals("25", params.get("age"));
	}

	@Test
	public void nestedXmlBodyFieldsUseDottedPaths() {
		withContentType(ContentType.TEXT_XML.type());

		Map<String, String> params = ParameterExtractor.extract(
			URI.create("/test"), "<order><customer><name>Bob</name></customer></order>", headers);

		assertEquals("Bob", params.get("customer.name"));
	}

	@Test
	public void applicationXmlIsTreatedAsXmlToo() {
		withContentType("application/xml");

		Map<String, String> params = ParameterExtractor.extract(
			URI.create("/test"), "<developer><name>Bob</name></developer>", headers);

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void malformedXmlIsIgnoredAndLeavesTheOtherSourcesIntact() {
		withContentType(ContentType.TEXT_XML.type());

		Map<String, String> params = ParameterExtractor.extract(
			URI.create("/test?from=query"), "<not xml", headers);

		assertEquals("query", params.get("from"));
	}

}
