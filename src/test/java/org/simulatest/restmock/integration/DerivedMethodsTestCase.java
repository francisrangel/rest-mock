package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

/** HEAD and OPTIONS are answered from the routes that were registered, not stubbed twice. */
public class DerivedMethodsTestCase extends IntegrationTestBase {

	@Test
	public void headIsServedFromTheGetRoute() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Bob\"}");

		HttpResponse<String> response = sendRequest("/users/1", HttpMethod.HEAD);

		assertEquals(200, response.statusCode());
		assertEquals("", response.body(), "HEAD must not carry a body");
		assertEquals("14", response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElse(""));
		assertEquals("application/json", response.headers().firstValue(HttpHeader.CONTENT_TYPE).orElse(""));
	}

	@Test
	public void headResolvesPlaceholdersToReportTheRightLength() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");

		HttpResponse<String> response = sendRequest("/users/42", HttpMethod.HEAD);

		assertEquals("user 42".length(), Integer.parseInt(response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElseThrow()));
	}

	@Test
	public void anExplicitHeadStubWinsOverTheGetRoute() throws Exception {
		RestMock.whenGet("/test").thenReturnText("from get");
		RestMock.whenHead("/test").thenReturnText("from head").withStatus(204);

		HttpResponse<String> response = sendRequest("/test", HttpMethod.HEAD);

		assertEquals(204, response.statusCode());
	}

	@Test
	public void headStillNotFoundWhenNothingIsRegistered() throws Exception {
		HttpResponse<String> response = sendRequest("/nothing", HttpMethod.HEAD);

		assertEquals(404, response.statusCode());
	}

	@Test
	public void optionsIsAnsweredWithoutItsOwnStub() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{}");
		RestMock.whenDelete("/users/1").thenReturnText("gone");

		HttpResponse<String> response = sendRequest("/users/1", HttpMethod.OPTIONS);

		assertEquals(204, response.statusCode());
		assertEquals(
			Set.of("GET", "HEAD", "DELETE", "OPTIONS"),
			Set.of(response.headers().firstValue(HttpHeader.ALLOW).orElseThrow().split(",\s*")));
	}

	@Test
	public void optionsOnAnUnregisteredPathIsStillNotFound() throws Exception {
		HttpResponse<String> response = sendRequest("/nothing", HttpMethod.OPTIONS);

		assertEquals(404, response.statusCode());
		assertTrue(response.headers().firstValue(HttpHeader.ALLOW).isEmpty());
	}

	/** A path stubbed only for POST answers OPTIONS, but must not claim HEAD. */
	@Test
	public void headIsNotAdvertisedWithoutAGetRoute() throws Exception {
		RestMock.whenPost("/submit").thenReturnText("ok");

		HttpResponse<String> response = sendRequest("/submit", HttpMethod.OPTIONS);

		assertEquals(Set.of("POST", "OPTIONS"),
			Set.of(response.headers().firstValue(HttpHeader.ALLOW).orElseThrow().split(",\s*")));
	}

}
