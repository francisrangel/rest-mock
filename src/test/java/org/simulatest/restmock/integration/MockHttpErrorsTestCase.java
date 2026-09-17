package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

/** A status other than 200 travels with whatever body was stubbed. */
public class MockHttpErrorsTestCase extends IntegrationTestBase {

	@Test
	public void thenReturnErrorCodeWithMessageIsAPlainTextBodyWithThatStatus() throws Exception {
		RestMock.whenGet("/secret").thenReturnErrorCodeWithMessage(403, "Forbidden");

		HttpResponse<String> response = sendRequest("/secret", HttpMethod.GET);

		assertEquals(403, response.statusCode());
		assertEquals("Forbidden", response.body());
		assertEquals(ContentType.TEXT_PLAIN.type() + "; charset=utf-8", contentType(response));
	}

	@Test
	public void withStatusAppliesToAJsonBody() throws Exception {
		RestMock.whenPost("/users").thenReturnJSON("{\"error\":\"invalid\"}").withStatus(422);

		HttpResponse<String> response = sendRequest("/users", HttpMethod.POST);

		assertEquals(422, response.statusCode());
		assertEquals("{\"error\":\"invalid\"}", response.body());
	}

	@Test
	public void withStatusAppliesToAnXmlBody() throws Exception {
		RestMock.whenGet("/data").thenReturnXML("<error>not found</error>").withStatus(404);

		HttpResponse<String> response = sendRequest("/data", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertEquals("<error>not found</error>", response.body());
		assertEquals(ContentType.TEXT_XML.type() + "; charset=utf-8", contentType(response));
	}

}
