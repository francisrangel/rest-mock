package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

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

	protected static final HttpClient client = TestHttp.CLIENT;

	/** A request builder already pointed at {@code path} on the running server. */
	protected HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
	}

	protected static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return TestHttp.send(request);
	}

	/** Sends the request and asserts the response body. Every helper below takes a path, not a full URL. */
	protected void assertResponseBody(String path, String expectedBody, HttpMethod method) throws Exception {
		HttpResponse<String> response = sendRequest(path, method);

		assertEquals(200, response.statusCode(), "unexpected status for " + method + " " + path);
		assertEquals(expectedBody, response.body());
	}

	/** Sends {@code requestBody} with the given Content-Type and asserts the response body. */
	protected void assertResponseBody(String path, HttpMethod method, String contentType, String requestBody, String expectedBody) throws Exception {
		HttpResponse<String> response = sendRequest(path, method, contentType, requestBody);

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

	protected static String contentType(HttpResponse<?> response) {
		return response.headers().firstValue(HttpHeader.CONTENT_TYPE).orElseThrow();
	}

	/** The comma-separated method list in {@code header}, as a set so its order is not asserted. */
	protected static Set<String> methods(HttpResponse<?> response, String header) {
		String value = response.headers().firstValue(header).orElse(null);

		assertNotNull(value, () -> "expected a " + header + " header, but the response sent none: " + response.headers().map());

		return Set.of(value.split(",\\s*"));
	}

}
