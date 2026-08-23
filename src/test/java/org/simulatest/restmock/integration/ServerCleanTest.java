package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

public class ServerCleanTest extends IntegrationTestBase {

	@Test
	public void routesShouldBeEliminatedOnClean() throws Exception {
		RestMock.whenGet("/test").thenReturnJSON("{ my: test }");
		assertResponseBody("/test", "{ my: test }", HttpMethod.GET);

		RestMock.clean();

		HttpResponse<String> response = sendRequest("/test", HttpMethod.GET);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
	}

	@Test
	public void theRequestLogShouldBeClearedOnClean() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");
		sendRequest("/test", HttpMethod.GET);
		assertEquals(1, RestMock.requests().count());

		RestMock.clean();

		assertTrue(RestMock.requests().isEmpty(), "clean() must clear the request log as well as the routes");
	}

}
