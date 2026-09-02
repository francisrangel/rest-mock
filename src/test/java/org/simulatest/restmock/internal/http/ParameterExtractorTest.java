package org.simulatest.restmock.internal.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.ReceivedRequest;
import org.simulatest.restmock.internal.Placeholders;
import org.simulatest.restmock.internal.response.ContentType;

public class ParameterExtractorTest {

	/** The type the JDK server hands FrontController, so names arrive canonicalized the way production sees them. */
	private Headers headers = new Headers();

	private void withContentType(String contentType) {
		headers.put(HttpHeader.CONTENT_TYPE, List.of(contentType));
	}

	/** What FrontController hands the extractor: the request as recorded, {@code headers} included. */
	private Map<String, String> extract(String uri, String body) {
		URI parsed = URI.create(uri);
		return ParameterExtractor.extract(
			new ReceivedRequest(HttpMethod.POST, parsed.getPath(), parsed.getRawQuery(), headers, body, Instant.now()));
	}

	/** Looks a name up the way a template does, so a header spelled either way resolves. */
	private static String resolve(Map<String, String> params, String name) {
		return params.get(Placeholders.key(name));
	}

	/**
	 * The map used to fold case for every name, so ${Name} quietly found a query
	 * parameter called name while queryParam("Name") on the same request found
	 * nothing. Bare names now match exactly, as the accessor does.
	 */
	@Test
	public void bareNamesAreMatchedExactly() {
		Map<String, String> params = extract("/test?name=Bob", "");

		assertEquals("Bob", params.get("name"));
		assertNull(params.get("Name"), "a bare name must not fold case");
	}

	@Test
	public void noQueryNoBody() {
		Map<String, String> params = extract("/test", "");

		assertTrue(params.isEmpty());
	}

	@Test
	public void singleQueryParameter() {
		Map<String, String> params = extract("/test?name=Bob", "");

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void multipleQueryParameters() {
		Map<String, String> params = extract("/test?a=1&b=2&c=3", "");

		assertEquals("1", params.get("a"));
		assertEquals("2", params.get("b"));
		assertEquals("3", params.get("c"));
	}

	@Test
	public void urlEncodedQueryParameter() {
		Map<String, String> params = extract("/test?msg=hello%20world", "");

		assertEquals("hello world", params.get("msg"));
	}

	@Test
	public void queryParameterWithoutValueIsSkipped() {
		Map<String, String> params = extract("/test?flag&name=Bob", "");

		assertEquals(1, params.size());
		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void formEncodedBody() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.type() + "; charset=UTF-8");

		Map<String, String> params = extract("/test", "name=Bob&age=25");

		assertEquals("Bob", params.get("name"));
		assertEquals("25", params.get("age"));
	}

	@Test
	public void jsonBody() {
		withContentType(ContentType.APPLICATION_JSON.type());

		Map<String, String> params = extract("/test", "{\"user\":{\"name\":\"Bob\"}}");

		assertEquals("Bob", params.get("user.name"));
	}

	@Test
	public void unknownContentTypeIgnoresBody() {
		withContentType(ContentType.TEXT_PLAIN.type());

		Map<String, String> params = extract("/test", "name=Bob");

		assertNull(params.get("name"), "a text/plain body must not be parsed for parameters");
		// the request's own headers are parameters too, so the map is not empty
		assertEquals(ContentType.TEXT_PLAIN.type(), resolve(params, "header." + HttpHeader.CONTENT_TYPE));
	}

	@Test
	public void queryAndFormBodyMerge() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.type());

		Map<String, String> params = extract("/test?from=query", "from_body=yes");

		assertEquals("query", params.get("from"));
		assertEquals("yes", params.get("from_body"));
	}

	@Test
	public void formBodyOverridesQueryWhenSameKey() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.type());

		Map<String, String> params = extract("/test?name=Alice", "name=Bob");

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void trailingAmpersandInQuery() {
		Map<String, String> params = extract("/test?name=Bob&", "");

		assertEquals("Bob", params.get("name"));
		assertEquals(1, params.size());
	}

	@Test
	public void valueContainingEqualsSign() {
		Map<String, String> params = extract("/test?expr=a%3Db", "");

		assertEquals("a=b", params.get("expr"));
	}

	@Test
	public void contentTypeCaseInsensitive() {
		withContentType("APPLICATION/JSON");

		Map<String, String> params = extract("/test", "{\"name\":\"Bob\"}");

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void emptyQueryString() {
		Map<String, String> params = extract("/test?", "");

		assertTrue(params.isEmpty());
	}

	// --- documented in RestMockResponse: "${name} may reference query parameters,
	// --- headers, JSON/XML body fields, and path captures"

	@Test
	public void headersAreAddressableUnderTheHeaderPrefix() {
		headers.add("X-Tenant", "acme");

		Map<String, String> params = extract("/test", "");

		assertEquals("acme", resolve(params, "header.X-Tenant"));
	}

	@Test
	public void theHeaderPrefixIsMatchedIgnoringCase() {
		// the JDK server rewrites X-Tenant to X-tenant, so a template that spells the
		// header the way the client sent it still has to resolve
		headers.add("X-Tenant", "acme");

		Map<String, String> params = extract("/test", "");

		assertEquals("acme", resolve(params, "header.X-Tenant"));
		assertEquals("acme", resolve(params, "HEADER.x-tenant"));
	}

	@Test
	public void aHeaderWithNoValueIsNotExposed() {
		headers.put("X-Empty", List.of());

		Map<String, String> params = extract("/test", "");

		assertNull(resolve(params, "header.X-Empty"));
	}

	@Test
	public void onlyTheFirstValueOfARepeatedHeaderIsExposed() {
		headers.put("X-Tenant", List.of("acme", "other"));

		Map<String, String> params = extract("/test", "");

		assertEquals("acme", resolve(params, "header.X-Tenant"));
	}

	/**
	 * The namespace holds what the stub author wrote, not what the HTTP client
	 * attached. A bare ${Accept} used to resolve to the client's Accept header,
	 * so a typo or a missing body field quietly produced a response instead of
	 * the "No value for ${...}" that every other unresolved name gets.
	 */
	@Test
	public void headersAreNotInTheBareNamespace() {
		headers.add("X-Tenant", "acme");
		headers.add("Accept", "*/*");

		Map<String, String> params = extract("/test", "");

		assertNull(params.get("X-Tenant"), "a bare ${X-Tenant} must not resolve a header");
		assertNull(params.get("Accept"), "a bare ${Accept} must not resolve a header");
	}

	@Test
	public void anAmbientHeaderCannotCollideWithAQueryParameter() {
		headers.add("name", "from-header");

		Map<String, String> params = extract("/test?name=from-query", "");

		assertEquals("from-query", params.get("name"));
		assertEquals("from-header", resolve(params, "header.name"));
	}

	@Test
	public void anAmbientHeaderCannotCollideWithABodyField() {
		withContentType(ContentType.APPLICATION_JSON.type());
		headers.add("name", "from-header");

		Map<String, String> params = extract("/test", "{\"name\":\"from-body\"}");

		assertEquals("from-body", params.get("name"));
		assertEquals("from-header", resolve(params, "header.name"));
	}

	@Test
	public void xmlBodyFieldsAreExposedAsParameters() {
		withContentType(ContentType.TEXT_XML.type());

		Map<String, String> params = extract("/test", "<developer><name>Bob</name><age>25</age></developer>");

		assertEquals("Bob", params.get("name"));
		assertEquals("25", params.get("age"));
	}

	@Test
	public void nestedXmlBodyFieldsUseDottedPaths() {
		withContentType(ContentType.TEXT_XML.type());

		Map<String, String> params = extract("/test", "<order><customer><name>Bob</name></customer></order>");

		assertEquals("Bob", params.get("customer.name"));
	}

	@Test
	public void applicationXmlIsTreatedAsXmlToo() {
		withContentType("application/xml");

		Map<String, String> params = extract("/test", "<developer><name>Bob</name></developer>");

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void malformedXmlIsIgnoredAndLeavesTheOtherSourcesIntact() {
		withContentType(ContentType.TEXT_XML.type());

		Map<String, String> params = extract("/test?from=query", "<not xml");

		assertEquals("query", params.get("from"));
	}

}
