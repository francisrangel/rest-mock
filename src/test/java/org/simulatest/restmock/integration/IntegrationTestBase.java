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

	/** Dogfooding RestMock.baseUrl(): if it stops pointing at the server, every case here fails. */
	protected final String baseUrl = RestMock.baseUrl();

	@RegisterExtension
	static RestMockExtension server = new RestMockExtension();

	protected static final HttpClient client = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	/** A request builder already pointed at {@code path} on the running server. */
	protected HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
	}

	protected static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** Sends the request and asserts the response body. Every helper below takes a path, not a full URL. */
	protected void assertResponseBody(String path, String expectedBody, HttpMethod method) throws Exception {
		HttpResponse<String> response = sendRequest(path, method);

		assertEquals(200, response.statusCode(), "unexpected status for " + method + " " + path);
		assertEquals(expectedBody, response.body());
	}

	protected HttpResponse<String> sendRequest(String path, HttpMethod method) throws Exception {
		return send(request(path).method(method.name(), HttpRequest.BodyPublishers.noBody()));
	}

	/** Sends {@code body} to {@code path} with the given method and Content-Type. */
	protected HttpResponse<String> sendRequest(String path, HttpMethod method, String contentType, String body) throws Exception {
		return send(request(path)
			.header(HttpHeader.CONTENT_TYPE, contentType)
			.method(method.name(), HttpRequest.BodyPublishers.ofString(body)));
	}

	/** Sends a GET to {@code path} and returns the raw response bytes. */
	protected HttpResponse<byte[]> getBytes(String path) throws Exception {
		return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
	}

}
