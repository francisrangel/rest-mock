package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.RestMockExtension;
import org.simulatest.restmock.internal.http.HttpHeader;

public class IntegrationTestBase {

	protected final String baseUrl = "http://localhost:" + RestMock.DEFAULT_PORT;

	@RegisterExtension
	static RestMockExtension server = new RestMockExtension();

	protected static final HttpClient client = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	/** Sends the request and asserts the response body. Every helper below takes a path, not a full URL. */
	protected void assertResponseBody(String path, String expectedBody, HttpMethod method) throws Exception {
		HttpResponse<String> response = sendRequest(path, method);

		assertEquals(200, response.statusCode(), "unexpected status for " + method + " " + path);
		assertEquals(expectedBody, response.body());
	}

	protected HttpResponse<String> sendRequest(String path, HttpMethod method) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + path))
			.method(method.name(), HttpRequest.BodyPublishers.noBody())
			.build();

		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	/** Sends {@code body} to {@code path} with the given method and Content-Type. */
	protected HttpResponse<String> sendRequest(String path, HttpMethod method, String contentType, String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + path))
			.header(HttpHeader.CONTENT_TYPE, contentType)
			.method(method.name(), HttpRequest.BodyPublishers.ofString(body))
			.build();

		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	/** Sends a GET to {@code path} and returns the raw response bytes. */
	protected HttpResponse<byte[]> getBytes(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

}
