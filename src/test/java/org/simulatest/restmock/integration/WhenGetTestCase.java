package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;
import org.simulatest.restmock.mock.Developer;

public class WhenGetTestCase extends IntegrationTestBase {

	@Test
	public void requestPlainText() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello World!");

		assertResponseBody("/test", "Hello World!", HttpMethod.GET);
	}

	@Test
	public void requestHtml() throws Exception {
		RestMock.whenGet("/test").thenReturnHTML("<h1>Mock rules</h1>");

		assertResponseBody("/test", "<h1>Mock rules</h1>", HttpMethod.GET);
	}

	@Test
	public void requestJSON() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";

		RestMock.whenGet("/test").thenReturnJSON(simpleJSON);

		assertResponseBody("/test", simpleJSON, HttpMethod.GET);
	}

	@Test
	public void requestJSONObject() throws Exception {
		RestMock.whenGet("/test").thenReturnJSON(new Developer("Bob", 25));

		assertResponseBody("/test", "{\"name\":\"Bob\",\"age\":25}", HttpMethod.GET);
	}

	@Test
	public void requestXML() throws Exception {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		RestMock.whenGet("/test").thenReturnXML(simpleXML);

		assertResponseBody("/test", simpleXML, HttpMethod.GET);
	}

	@Test
	public void requestXMLObject() throws Exception {
		RestMock.whenGet("/test").thenReturnXML(new Developer("Bob", 25));

		assertResponseBody("/test", "<Developer><name>Bob</name><age>25</age></Developer>", HttpMethod.GET);
	}

	@Test
	public void requestPlainTextGetWithParameters() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello ${name}!");

		assertResponseBody("/test?name=Bob", "Hello Bob!", HttpMethod.GET);
	}

	@Test
	public void requestPlainTextGetWithManyParameters() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello ${name}, you are the number #${number}!");

		assertResponseBody("/test?name=Bob&number=1", "Hello Bob, you are the number #1!", HttpMethod.GET);
	}

	@Test
	public void emptyBodyIsServedAsAnEmptyResponse() throws Exception {
		RestMock.whenGet("/empty").thenReturnText("");

		assertResponseBody("/empty", "", HttpMethod.GET);
	}

	@Test
	public void customHeaderArrivesOverHttp() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok")
			.withHeader("X-Custom", "hello")
			.withHeader("X-Trace-Id", "abc123");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals("hello", response.headers().firstValue("X-Custom").orElse(""));
		assertEquals("abc123", response.headers().firstValue("X-Trace-Id").orElse(""));
	}

	@Test
	public void anExplicitContentTypeHeaderOverridesTheBodyDefault() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok").withHeader(HttpHeader.CONTENT_TYPE, "application/vnd.custom");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals("application/vnd.custom", response.headers().firstValue(HttpHeader.CONTENT_TYPE).orElseThrow());
	}

	@Test
	public void lastValueWinsForARepeatedHeader() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok")
			.withHeader("X-Custom", "first")
			.withHeader("X-Custom", "second");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals(List.of("second"), response.headers().allValues("X-Custom"));
	}

	@Test
	public void corsHeadersAreSentOnEveryResponse() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals("*", response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).orElseThrow());
		assertEquals("360", response.headers().firstValue(HttpHeader.ACCESS_CONTROL_MAX_AGE).orElseThrow());
		assertEquals("x-requested-with", response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).orElseThrow());
		assertEquals("true", response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS).orElseThrow());
	}

	@Test
	public void unmatchedRequestsCarryNoCorsHeaders() throws Exception {
		HttpResponse<String> response = sendRequest("/nothing-here", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty(),
			"404 responses should not carry CORS headers");
	}

}
