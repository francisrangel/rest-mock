package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class JsonBodyTestCase extends IntegrationTestBase {

	@Test
	public void flatJsonKey() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		postJson("{\"name\":\"Bob\"}", "hello Bob");
	}

	@Test
	public void nestedJsonKey() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${user.name}");

		postJson("{\"user\":{\"name\":\"Bob\"}}", "hello Bob");
	}

	@Test
	public void arrayIndexInJson() throws Exception {
		RestMock.whenPost("/test").thenReturnText("first=${items.0.x}");

		postJson("{\"items\":[{\"x\":\"a\"},{\"x\":\"b\"}]}", "first=a");
	}

	@Test
	public void numericAndBooleanScalars() throws Exception {
		RestMock.whenPost("/test").thenReturnText("age=${age} active=${active}");

		postJson("{\"age\":25,\"active\":true}", "age=25 active=true");
	}

	/** A JSON body sent as text/plain is never parsed, so its fields are not available. */
	@Test
	public void nonJsonContentTypeMeansTheBodyFieldsAreNotAvailable() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		HttpResponse<String> response =
			sendRequest("/test", HttpMethod.POST, ContentType.TEXT_PLAIN.type(), "{\"name\":\"Bob\"}");

		assertEquals(500, response.statusCode());
		assertTrue(response.body().startsWith("No value for ${name}"), response.body());
	}

	@Test
	public void malformedJsonYieldsNoFieldsAndSaysSo() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		HttpResponse<String> response =
			sendRequest("/test", HttpMethod.POST, ContentType.APPLICATION_JSON.type(), "{not json");

		assertEquals(500, response.statusCode());
		assertTrue(response.body().startsWith("No value for ${name}"), response.body());
	}

	/** A body field that does resolve is escaped, so a quote cannot break the JSON. */
	@Test
	public void aValueWithAQuoteStaysValidJson() throws Exception {
		RestMock.whenPost("/echo").thenReturnJSON("{\"name\":\"${name}\"}");

		HttpResponse<String> response = sendRequest("/echo", HttpMethod.POST,
			ContentType.APPLICATION_JSON.type(), "{\"name\":\"Bob \\\"the builder\\\"\"}");

		assertEquals("{\"name\":\"Bob \\\"the builder\\\"\"}", response.body());
	}

	private void postJson(String jsonBody, String expectedAnswer) throws Exception {
		assertResponseBody("/test", HttpMethod.POST, ContentType.APPLICATION_JSON.type(), jsonBody, expectedAnswer);
	}

}
