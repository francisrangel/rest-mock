package restmock.integration;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import restmock.RestMock;
import restmock.http.HttpMethod;

public class MockHttpErrorsTestCase extends IntegrationTestBase {

	@Test
	public void returningError404() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello World!");

		HttpResponse<String> response = sendRequest(baseUrl + "/test1", HttpMethod.GET);

		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
	}

	@Test
	public void returningBadRequestForGETMethod() throws Exception {
		RestMock.whenGet("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_BAD_REQUEST, "Message for error 500 GET");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.GET);

		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode());
		assertEquals("Message for error 500 GET" + lineSeparator(), response.body());
	}

	@Test
	public void returningBadRequestForPOSTMethod() throws Exception {
		RestMock.whenPost("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_BAD_REQUEST, "Message for error 500 POST");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.POST);

		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode());
		assertEquals("Message for error 500 POST" + lineSeparator(), response.body());
	}

	@Test
	public void returningForbiddenForGETMethod() throws Exception {
		RestMock.whenGet("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_FORBIDDEN, "Forbidden GET");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.GET);

		assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.statusCode());
		assertEquals("Forbidden GET" + lineSeparator(), response.body());
	}

	@Test
	public void returningForbiddenForPOSTMethod() throws Exception {
		RestMock.whenPost("/test").thenReturnErrorCodeWithMessage(HttpURLConnection.HTTP_FORBIDDEN, "Forbidden POST");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.POST);

		assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.statusCode());
		assertEquals("Forbidden POST" + lineSeparator(), response.body());
	}

	@Test
	public void jsonBodyWithCreatedStatus() throws Exception {
		RestMock.whenPost("/users").thenReturnJSON("{\"id\":1}").withStatus(201);

		HttpResponse<String> response = sendRequest(baseUrl + "/users", HttpMethod.POST);

		assertEquals(201, response.statusCode());
		assertEquals("{\"id\":1}" + lineSeparator(), response.body());
	}

	@Test
	public void jsonBodyWithUnprocessableEntityStatus() throws Exception {
		RestMock.whenPost("/users").thenReturnJSON("{\"error\":\"invalid\"}").withStatus(422);

		HttpResponse<String> response = sendRequest(baseUrl + "/users", HttpMethod.POST);

		assertEquals(422, response.statusCode());
		assertEquals("{\"error\":\"invalid\"}" + lineSeparator(), response.body());
	}

	@Test
	public void xmlBodyWithCustomStatus() throws Exception {
		RestMock.whenGet("/data").thenReturnXML("<error>not found</error>").withStatus(404);

		HttpResponse<String> response = sendRequest(baseUrl + "/data", HttpMethod.GET);

		assertEquals(404, response.statusCode());
	}

}
