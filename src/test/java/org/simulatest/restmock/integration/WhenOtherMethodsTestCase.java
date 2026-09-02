package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

public class WhenOtherMethodsTestCase extends IntegrationTestBase {

	@Test
	public void put() throws Exception {
		RestMock.whenPut("/test").thenReturnText("Put succeed");

		assertResponseBody("/test", "Put succeed", HttpMethod.PUT);
	}

	@Test
	public void delete() throws Exception {
		RestMock.whenDelete("/test").thenReturnText("Delete succeed");

		assertResponseBody("/test", "Delete succeed", HttpMethod.DELETE);
	}

	@Test
	public void patch() throws Exception {
		RestMock.whenPatch("/test").thenReturnText("Patch succeed");

		assertResponseBody("/test", "Patch succeed", HttpMethod.PATCH);
	}

	@Test
	public void options() throws Exception {
		RestMock.whenOptions("/test").thenReturnText("Options succeed");

		assertResponseBody("/test", "Options succeed", HttpMethod.OPTIONS);
	}

	@Test
	public void optionsAllowHeaderListsRegisteredMethodsForPath() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");
		RestMock.whenPost("/test").thenReturnText("ok");
		RestMock.whenOptions("/test").thenReturnText("ok");
		RestMock.whenGet("/other").thenReturnText("ok");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.OPTIONS);

		String allow = response.headers().firstValue(HttpHeader.ALLOW).orElse("");
		Set<String> methods = Set.of(allow.split(",\\s*"));
		assertEquals(
			Set.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name()),
			methods,
			"HEAD is advertised because the path answers GET");
	}

	@Test
	public void head() throws Exception {
		RestMock.whenHead("/test").thenReturnText("Head succeed");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.HEAD);

		assertEquals(200, response.statusCode());
		assertEquals("", response.body());
		assertEquals("12", response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElse(""));
	}

	@Test
	public void headWithANoContentStatusOmitsContentLength() throws Exception {
		RestMock.whenHead("/test").thenReturnText("Head succeed").withStatus(204);

		HttpResponse<String> response = sendRequest("/test", HttpMethod.HEAD);

		assertEquals(204, response.statusCode());
		assertTrue(response.headers().firstValue(HttpHeader.CONTENT_LENGTH).isEmpty(),
			"a 204 must not declare a Content-Length, the JDK server rejects the exchange");
	}

	@Test
	public void anUnsupportedVerbGetsNotImplemented() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");

		HttpResponse<String> response = client.send(
			request("/test")
				.method("TRACE", HttpRequest.BodyPublishers.noBody())
				.build(),
			HttpResponse.BodyHandlers.ofString());

		assertEquals(501, response.statusCode());
	}

}
