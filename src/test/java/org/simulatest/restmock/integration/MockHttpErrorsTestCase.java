package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class MockHttpErrorsTestCase extends IntegrationTestBase {

	@Test
	public void returningError404() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello World!");

		HttpResponse<String> response = sendRequest("/test1", HttpMethod.GET);

		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
	}

	@Test
	public void returningBadRequestForGETMethod() throws Exception {
		RestMock.whenGet("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_BAD_REQUEST, "bad request from GET");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode());
		assertEquals("bad request from GET", response.body());
	}

	@Test
	public void returningBadRequestForPOSTMethod() throws Exception {
		RestMock.whenPost("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_BAD_REQUEST, "bad request from POST");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.POST);

		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode());
		assertEquals("bad request from POST", response.body());
	}

	@Test
	public void returningForbiddenForGETMethod() throws Exception {
		RestMock.whenGet("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_FORBIDDEN, "Forbidden GET");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);

		assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.statusCode());
		assertEquals("Forbidden GET", response.body());
	}

	@Test
	public void returningForbiddenForPOSTMethod() throws Exception {
		RestMock.whenPost("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_FORBIDDEN, "Forbidden POST");

		HttpResponse<String> response = sendRequest("/test", HttpMethod.POST);

		assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.statusCode());
		assertEquals("Forbidden POST", response.body());
	}

	@Test
	public void jsonBodyWithCreatedStatus() throws Exception {
		RestMock.whenPost("/users").thenReturnJSON("{\"id\":1}").withStatus(201);

		HttpResponse<String> response = sendRequest("/users", HttpMethod.POST);

		assertEquals(201, response.statusCode());
		assertEquals("{\"id\":1}", response.body());
	}

	@Test
	public void jsonBodyWithUnprocessableEntityStatus() throws Exception {
		RestMock.whenPost("/users").thenReturnJSON("{\"error\":\"invalid\"}").withStatus(422);

		HttpResponse<String> response = sendRequest("/users", HttpMethod.POST);

		assertEquals(422, response.statusCode());
		assertEquals("{\"error\":\"invalid\"}", response.body());
	}

	@Test
	public void xmlBodyWithCustomStatus() throws Exception {
		RestMock.whenGet("/data").thenReturnXML("<error>not found</error>").withStatus(404);

		HttpResponse<String> response = sendRequest("/data", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertEquals("<error>not found</error>", response.body());
		assertEquals(ContentType.TEXT_XML.type() + "; charset=utf-8", contentType(response));
	}

}
