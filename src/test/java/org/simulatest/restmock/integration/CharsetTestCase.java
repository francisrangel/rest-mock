package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

/**
 * Bodies are encoded UTF-8, so text responses have to say so. Without the
 * charset a client falling back to the historical text/* default decodes
 * non-ASCII as mojibake.
 */
public class CharsetTestCase extends IntegrationTestBase {

	@Test
	public void nonAsciiTextSurvivesTheRoundTrip() throws Exception {
		RestMock.whenGet("/accent").thenReturnText("café não");

		assertEquals("café não", sendRequest("/accent", HttpMethod.GET).body());
	}

	@Test
	public void nonAsciiJsonSurvivesTheRoundTrip() throws Exception {
		RestMock.whenGet("/city").thenReturnJSON("{\"city\":\"São Paulo\"}");

		assertEquals("{\"city\":\"São Paulo\"}", sendRequest("/city", HttpMethod.GET).body());
	}

	@Test
	public void placeholderValuesSurviveTheRoundTrip() throws Exception {
		RestMock.whenGet("/hello/{name}").thenReturnText("olá ${name}");

		assertEquals("olá José", sendRequest("/hello/Jos%C3%A9", HttpMethod.GET).body());
	}

	@Test
	public void textTypesDeclareUtf8() throws Exception {
		RestMock.whenGet("/text").thenReturnText("ok");
		RestMock.whenGet("/json").thenReturnJSON("{}");
		RestMock.whenGet("/html").thenReturnHTML("<p>ok</p>");

		assertEquals("text/plain; charset=utf-8", contentTypeOf("/text"));
		assertEquals("application/json; charset=utf-8", contentTypeOf("/json"));
		assertEquals("text/html; charset=utf-8", contentTypeOf("/html"));
	}

	/** Raw bytes carry no known encoding, so nothing is claimed for them. */
	@Test
	public void binaryTypesDeclareNoCharset() throws Exception {
		RestMock.whenGet("/bin").thenReturnFile(new byte[] { 1, 2, 3 });

		assertEquals("application/octet-stream", contentTypeOf("/bin"));
	}

	@Test
	public void anExplicitContentTypeHeaderStillWins() throws Exception {
		RestMock.whenGet("/custom").thenReturnText("ok").withHeader(HttpHeader.CONTENT_TYPE, "text/plain; charset=iso-8859-1");

		assertEquals("text/plain; charset=iso-8859-1", contentTypeOf("/custom"));
	}

	private String contentTypeOf(String path) throws Exception {
		HttpResponse<String> response = sendRequest(path, HttpMethod.GET);
		return response.headers().firstValue(HttpHeader.CONTENT_TYPE).orElseThrow();
	}

}
