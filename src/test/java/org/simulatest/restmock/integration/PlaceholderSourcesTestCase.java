package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

/**
 * End-to-end coverage for every {@code ${name}} source documented on RestMockResponse:
 * path captures, body fields, query parameters and request headers.
 */
public class PlaceholderSourcesTestCase extends IntegrationTestBase {

	@Test
	public void headersResolvePlaceholders() throws Exception {
		RestMock.whenGet("/whoami").thenReturnText("tenant=${X-Tenant}");

		HttpResponse<String> response = client.send(
			HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/whoami"))
				.header("X-Tenant", "acme")
				.GET().build(),
			HttpResponse.BodyHandlers.ofString());

		assertEquals("tenant=acme", response.body());
	}

	@Test
	public void xmlBodyFieldsResolvePlaceholders() throws Exception {
		RestMock.whenPost("/orders").thenReturnText("hello ${name}, you are ${age}");

		HttpResponse<String> response = sendRequest("/orders", HttpMethod.POST,
			ContentType.TEXT_XML.type(), "<developer><name>Bob</name><age>25</age></developer>");

		assertEquals("hello Bob, you are 25", response.body());
	}

	@Test
	public void nestedXmlBodyFieldsResolveByDottedPath() throws Exception {
		RestMock.whenPost("/orders").thenReturnText("customer=${customer.name}");

		HttpResponse<String> response = sendRequest("/orders", HttpMethod.POST,
			ContentType.TEXT_XML.type(), "<order><customer><name>Bob</name></customer></order>");

		assertEquals("customer=Bob", response.body());
	}

	@Test
	public void everySourceCanBeMixedInOneBody() throws Exception {
		RestMock.whenPost("/users/{id}").thenReturnText("${id}|${from}|${name}|${X-Tenant}");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + "/users/42?from=web"))
			.header("X-Tenant", "acme")
			.header("Content-Type", ContentType.APPLICATION_JSON.type())
			.POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Bob\"}"))
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals("42|web|Bob|acme", response.body());
	}

	@Test
	public void pathCapturesWinOverEveryOtherSource() throws Exception {
		RestMock.whenPost("/users/{name}").thenReturnText("${name}");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + "/users/from-path?name=from-query"))
			.header("name", "from-header")
			.header("Content-Type", ContentType.APPLICATION_JSON.type())
			.POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"from-body\"}"))
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals("from-path", response.body());
	}

	/**
	 * A typo used to ship as literal ${nobody} in the body, so a test asserting
	 * only the status still passed. It now fails, and the message lists what
	 * was actually available.
	 */
	@Test
	public void anUnknownPlaceholderFailsLoudlyAndListsWhatWasAvailable() throws Exception {
		RestMock.whenGet("/test").thenReturnText("hello ${nobody}");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals(500, response.statusCode());
		assertTrue(response.body().startsWith("No value for ${nobody}. Available names:"), response.body());
		assertTrue(response.body().contains("Host"), "the available names should include the headers: " + response.body());
	}

}
