package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.RestMockExtension;

/**
 * Does not extend IntegrationTestBase: the point of this class is that the
 * extension binds the port it was given rather than one the OS picks.
 */
public class CustomPortTestCase {

	private static final int PORT = 9081;

	@RegisterExtension
	static RestMockExtension restMock = new RestMockExtension(PORT);

	@Test
	public void theServerAnswersOnTheConfiguredPort() throws Exception {
		RestMock.whenGet("/test").thenReturnText("custom port");

		HttpResponse<String> response = TestHttp.get("http://localhost:" + PORT + "/test");

		assertEquals(200, response.statusCode());
		assertEquals("custom port", response.body());
	}

}
