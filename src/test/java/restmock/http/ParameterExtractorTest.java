package restmock.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import restmock.response.ContentType;

public class ParameterExtractorTest {

	private final HttpExchange exchange = mock(HttpExchange.class);
	private final Headers requestHeaders = new Headers();

	private void prepare(String uri) {
		when(exchange.getRequestURI()).thenReturn(URI.create(uri));
		when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
	}

	private void withBody(String contentType, String body) {
		requestHeaders.set(HttpHeader.CONTENT_TYPE, contentType);
		when(exchange.getRequestBody())
			.thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void noQueryNoBody() throws IOException {
		prepare("/test");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test"));

		assertTrue(params.isEmpty());
	}

	@Test
	public void singleQueryParameter() throws IOException {
		prepare("/test?name=Bob");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?name=Bob"));

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void multipleQueryParameters() throws IOException {
		prepare("/test?a=1&b=2&c=3");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?a=1&b=2&c=3"));

		assertEquals("1", params.get("a"));
		assertEquals("2", params.get("b"));
		assertEquals("3", params.get("c"));
	}

	@Test
	public void urlEncodedQueryParameter() throws IOException {
		prepare("/test?msg=hello%20world");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?msg=hello%20world"));

		assertEquals("hello world", params.get("msg"));
	}

	@Test
	public void queryParameterWithoutValueIsSkipped() throws IOException {
		prepare("/test?flag&name=Bob");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?flag&name=Bob"));

		assertEquals(1, params.size());
		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void formEncodedBody() throws IOException {
		prepare("/test");
		withBody(ContentType.APPLICATION_FORM_URLENCODED.getType() + "; charset=UTF-8", "name=Bob&age=25");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test"));

		assertEquals("Bob", params.get("name"));
		assertEquals("25", params.get("age"));
	}

	@Test
	public void jsonBody() throws IOException {
		prepare("/test");
		withBody(ContentType.APPLICATION_JSON.getType(), "{\"user\":{\"name\":\"Bob\"}}");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test"));

		assertEquals("Bob", params.get("user.name"));
	}

	@Test
	public void unknownContentTypeIgnoresBody() throws IOException {
		prepare("/test");
		withBody(ContentType.TEXT_PLAIN.getType(), "name=Bob");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test"));

		assertTrue(params.isEmpty());
	}

	@Test
	public void queryAndFormBodyMerge() throws IOException {
		prepare("/test?from=query");
		withBody(ContentType.APPLICATION_FORM_URLENCODED.getType(), "from_body=yes");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?from=query"));

		assertEquals("query", params.get("from"));
		assertEquals("yes", params.get("from_body"));
	}

	@Test
	public void formBodyOverridesQueryWhenSameKey() throws IOException {
		prepare("/test?name=Alice");
		withBody(ContentType.APPLICATION_FORM_URLENCODED.getType(), "name=Bob");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?name=Alice"));

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void trailingAmpersandInQuery() throws IOException {
		prepare("/test?name=Bob&");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?name=Bob&"));

		assertEquals("Bob", params.get("name"));
		assertEquals(1, params.size());
	}

	@Test
	public void valueContainingEqualsSign() throws IOException {
		prepare("/test?expr=a%3Db");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?expr=a%3Db"));

		assertEquals("a=b", params.get("expr"));
	}

	@Test
	public void contentTypeCaseInsensitive() throws IOException {
		prepare("/test");
		withBody("APPLICATION/JSON", "{\"name\":\"Bob\"}");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test"));

		assertEquals("Bob", params.get("name"));
	}

	@Test
	public void emptyQueryString() throws IOException {
		prepare("/test?");

		Map<String, String> params = ParameterExtractor.extract(exchange, URI.create("/test?"));

		assertTrue(params.isEmpty());
	}

}
