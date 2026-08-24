package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

/**
 * The 404 a mismatched stub produces is the most-read output this library has.
 * These cover what it must say for "why isn't my mock matching?" to answer itself.
 */
public class NoStubDiagnosticsTestCase extends IntegrationTestBase {

	@Test
	public void anUnmatchedRequestIsToldWhatIsStubbed() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{}");
		RestMock.whenPost("/orders").thenReturnText("ok");

		HttpResponse<String> response = sendRequest("/users/01", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertTrue(response.body().contains("No stub for GET /users/01"), response.body());
		assertTrue(response.body().contains("Closest stub: GET /users/1"), response.body());
		assertTrue(response.body().contains("POST    /orders"), response.body());
	}

	@Test
	public void callingAStubbedPathWithTheWrongVerbSaysSo() throws Exception {
		RestMock.whenPost("/users").thenReturnText("created");

		HttpResponse<String> response = sendRequest("/users", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertTrue(response.body().contains("/users is stubbed for POST, not GET."), response.body());
	}

	@Test
	public void theDiagnosticIsPlainTextTheTerminalCanPrint() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{}");

		HttpResponse<String> response = sendRequest("/nowhere", HttpMethod.GET);

		assertEquals("text/plain; charset=utf-8",
			response.headers().firstValue(HttpHeader.CONTENT_TYPE).orElseThrow());
	}

	/** HEAD promises the length of a body it never sends, 404 included. */
	@Test
	public void aHeadRequestGetsTheLengthButNoBody() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{}");

		HttpResponse<String> response = sendRequest("/nowhere", HttpMethod.HEAD);

		assertEquals(404, response.statusCode());
		assertEquals("", response.body());
		assertTrue(Integer.parseInt(response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElseThrow()) > 0);
	}

	@Test
	public void optionsOnAnUnstubbedPathIsExplainedToo() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{}");

		HttpResponse<String> response = sendRequest("/nowhere", HttpMethod.OPTIONS);

		assertEquals(404, response.statusCode());
		assertTrue(response.body().contains("No stub for OPTIONS /nowhere"), response.body());
	}

	@Test
	public void aStubUriWithAQueryStringFailsAtTheStubNotAtTheRequest() {
		IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
			() -> RestMock.whenGet("/users?active=true"));

		assertTrue(rejected.getMessage().contains("must not contain a query string"), rejected.getMessage());
	}

	@Test
	public void aStubUriWithoutALeadingSlashFailsAtTheStub() {
		assertThrows(IllegalArgumentException.class, () -> RestMock.whenGet("users/1"));
	}

	/** The rejected stub must not be half-registered and answering later requests. */
	@Test
	public void aRejectedStubRegistersNothing() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> RestMock.whenGet("/users/{id"));

		HttpResponse<String> response = sendRequest("/users/1", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertTrue(response.body().contains("Nothing is stubbed"), response.body());
	}

}
