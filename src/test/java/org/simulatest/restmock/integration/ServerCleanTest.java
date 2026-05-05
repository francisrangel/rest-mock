package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

public class ServerCleanTest extends IntegrationTestBase {

	@Test
	public void routesShouldBeEliminatedOnClean() throws Exception {
		RestMock.whenGet("/test").thenReturnJSON("{ my: test }");
		requestMethodWithResultString(baseUrl + "/test", "{ my: test }", HttpMethod.GET);

		RestMock.clean();

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.GET);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
	}

}
