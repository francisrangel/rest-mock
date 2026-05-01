package restmock.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import restmock.response.ContentType;

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
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.getType() + "; charset=UTF-8");

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "name=Bob&age=25", headers);

		assertEquals("Bob", params.get("name"));
		assertEquals("25", params.get("age"));
	}

	@Test
	public void jsonBody() {
		withContentType(ContentType.APPLICATION_JSON.getType());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "{\"user\":{\"name\":\"Bob\"}}", headers);

		assertEquals("Bob", params.get("user.name"));
	}

	@Test
	public void unknownContentTypeIgnoresBody() {
		withContentType(ContentType.TEXT_PLAIN.getType());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test"), "name=Bob", headers);

		assertTrue(params.isEmpty());
	}

	@Test
	public void queryAndFormBodyMerge() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.getType());

		Map<String, String> params = ParameterExtractor.extract(URI.create("/test?from=query"), "from_body=yes", headers);

		assertEquals("query", params.get("from"));
		assertEquals("yes", params.get("from_body"));
	}

	@Test
	public void formBodyOverridesQueryWhenSameKey() {
		withContentType(ContentType.APPLICATION_FORM_URLENCODED.getType());

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

}
