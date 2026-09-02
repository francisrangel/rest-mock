package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class DanglingRouteTestCase extends IntegrationTestBase {

	@Test
	public void danglingRouteReturns501WithDiagnosticMessage() throws Exception {
		RestMock.whenGet("/forgot");

		HttpResponse<String> response = sendRequest("/forgot", HttpMethod.GET);

		assertEquals(501, response.statusCode());
		assertTrue(response.body().contains("/forgot"), "body was: " + response.body());
		assertTrue(response.body().contains("no response was configured"), "body was: " + response.body());
	}

	@Test
	public void thenReturnReplacesSentinel() throws Exception {
		RestMock.whenGet("/completed").thenReturnText("ok");

		HttpResponse<String> response = sendRequest("/completed", HttpMethod.GET);

		assertEquals(200, response.statusCode());
		assertEquals("ok", response.body());
		assertEquals(ContentType.TEXT_PLAIN.type() + "; charset=utf-8", contentType(response));
	}

}
