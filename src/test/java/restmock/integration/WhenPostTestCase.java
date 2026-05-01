package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Test;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class WhenPostTestCase extends IntegrationTestBase {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	@Test
	public void postWithoutParametersWithPlainTextResponse() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Post succeed");

		requestPostWithResultString("Post succeed");
	}

	@Test
	public void postWithOneParameter() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}!");

		requestPostWithParameters(baseUrl + "/test", "name=Bob", "Hello Bob!");
	}

	@Test
	public void postWithManyParamters() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}! You are the number #${number} of #${total}.");

		requestPostWithParameters(baseUrl + "/test", "name=Bob&number=1&total=10", "Hello Bob! You are the number #1 of #10.");
	}

	private void requestPostWithResultString(String expectedBody) throws Exception {
		requestMethodWithResultString(baseUrl + "/test", expectedBody, HttpMethod.POST);
	}

	private void requestPostWithParameters(String url, String requestParametersString, String resultString) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
			.POST(HttpRequest.BodyPublishers.ofString(requestParametersString))
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals(resultString + LINE_SEPARATOR, response.body());
	}

}
