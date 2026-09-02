package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

/**
 * End-to-end coverage for every {@code ${name}} source documented on RestMockResponse:
 * path captures, body fields, query parameters and request headers.
 *
 * Headers live under the {@code header.} prefix; the bare namespace is reserved
 * for what the stub author wrote.
 */
public class PlaceholderSourcesTestCase extends IntegrationTestBase {

	@Test
	public void headersResolvePlaceholders() throws Exception {
		RestMock.whenGet("/whoami").thenReturnText("tenant=${header.X-Tenant}");

		HttpResponse<String> response = send(request("/whoami").header("X-Tenant", "acme").GET());

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
		RestMock.whenPost("/users/{id}").thenReturnText("${id}|${from}|${name}|${header.X-Tenant}");

		HttpRequest request = request("/users/42?from=web")
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

		HttpRequest request = request("/users/from-path?name=from-query")
			.header("name", "from-header")
			.header("Content-Type", ContentType.APPLICATION_JSON.type())
			.POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"from-body\"}"))
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals("from-path", response.body());
	}

	/**
	 * A response loaded from a resource is still a template. The README says so,
	 * and nothing covered it: every FromResource test used a fixture with no
	 * placeholder in it, so the two features had never been exercised together.
	 */
	@Test
	public void placeholdersInAResourceLoadedBodyStillResolve() throws Exception {
		RestMock.whenGet("/greet/{id}").thenReturnTextFromResource("greeting.txt");

		HttpResponse<String> response = sendRequest("/greet/42?name=Bob", HttpMethod.GET);

		assertEquals("hello Bob, you asked for user 42", response.body());
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
		assertTrue(response.body().contains("request headers as ${header.NAME}"),
			"the message should point at the header namespace: " + response.body());
	}

	/**
	 * The diagnostic lists what the author wrote and only counts the headers.
	 * Spelling out Host, User-agent, Accept and the rest buried the two names
	 * anybody is actually looking for.
	 */
	@Test
	public void theUnknownPlaceholderMessageDoesNotDrownInHeaders() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("${nobody}");

		HttpResponse<String> response = sendRequest("/users/42?from=web", HttpMethod.GET);

		String message = response.body();

		assertTrue(message.contains("id"), message);
		assertTrue(message.contains("from"), message);
		assertFalse(message.contains("User-agent"), "headers should be counted, not listed: " + message);
	}

	/**
	 * The README's own example: a percent-encoded quote in the path is decoded
	 * on the way in and escaped on the way out, so the JSON stays well-formed.
	 */
	@Test
	public void anEncodedPathCaptureIsDecodedThenEscapedForJson() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnJSON("{\"id\":\"${id}\"}");

		HttpResponse<String> response = sendRequest("/users/a%22b", HttpMethod.GET);

		assertEquals("{\"id\":\"a\\\"b\"}", response.body());
	}

	@Test
	public void aBareNameMustMatchTheCaseTheRequestUsed() throws Exception {
		RestMock.whenGet("/test").thenReturnText("hello ${Name}");

		HttpResponse<String> response = sendRequest("/test?name=Bob", HttpMethod.GET);

		assertEquals(500, response.statusCode());
		assertTrue(response.body().startsWith("No value for ${Name}. Available names: name"), response.body());
	}

	/**
	 * The hole the header namespace closed: a bare ${Accept} used to resolve to
	 * whatever the HTTP client attached, so a name the author never defined
	 * produced a cheerful 200 instead of the loud failure every other unresolved
	 * name gets.
	 */
	@Test
	public void aBareHeaderNameNoLongerResolvesSilently() throws Exception {
		RestMock.whenGet("/test").thenReturnText("accept=${Accept}");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals(500, response.statusCode());
		assertTrue(response.body().startsWith("No value for ${Accept}."), response.body());
	}

}
