package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.RestMockExtension;

/**
 * Does not extend IntegrationTestBase: the point of this class is that the
 * extension binds a port other than {@link RestMock#DEFAULT_PORT}.
 */
public class CustomPortTestCase {

	private static final int PORT = 9081;

	@RegisterExtension
	static RestMockExtension server = new RestMockExtension(PORT);

	private static final HttpClient client = HttpClient.newHttpClient();

	@Test
	public void theServerAnswersOnTheConfiguredPort() throws Exception {
		RestMock.whenGet("/test").thenReturnText("custom port");

		HttpResponse<String> response = client.send(
			HttpRequest.newBuilder().uri(URI.create("http://localhost:" + PORT + "/test")).GET().build(),
			HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals("custom port", response.body());
	}

}
