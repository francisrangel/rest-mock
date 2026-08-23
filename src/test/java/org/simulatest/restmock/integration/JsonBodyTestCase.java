package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class JsonBodyTestCase extends IntegrationTestBase {

	@Test
	public void flatJsonKey() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		postJson("/test", "{\"name\":\"Bob\"}", "hello Bob");
	}

	@Test
	public void nestedJsonKey() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${user.name}");

		postJson("/test", "{\"user\":{\"name\":\"Bob\"}}", "hello Bob");
	}

	@Test
	public void arrayIndexInJson() throws Exception {
		RestMock.whenPost("/test").thenReturnText("first=${items.0.x}");

		postJson("/test", "{\"items\":[{\"x\":\"a\"},{\"x\":\"b\"}]}", "first=a");
	}

	@Test
	public void numericAndBooleanScalars() throws Exception {
		RestMock.whenPost("/test").thenReturnText("age=${age} active=${active}");

		postJson("/test", "{\"age\":25,\"active\":true}", "age=25 active=true");
	}

	@Test
	public void nonJsonContentTypeLeavesPlaceholderLiteral() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		HttpResponse<String> response =
			sendRequest("/test", HttpMethod.POST, ContentType.TEXT_PLAIN.type(), "{\"name\":\"Bob\"}");

		assertEquals("hello ${name}", response.body());
	}

	@Test
	public void malformedJsonIsIgnoredAndRouteStillResponds() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		postJson("/test", "{not json", "hello ${name}");
	}

	private void postJson(String path, String jsonBody, String expectedAnswer) throws Exception {
		HttpResponse<String> response =
			sendRequest(path, HttpMethod.POST, ContentType.APPLICATION_JSON.type(), jsonBody);

		assertEquals(200, response.statusCode());
		assertEquals(expectedAnswer, response.body());
	}

}
