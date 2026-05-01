package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import org.junit.Test;

import restmock.RestMock;
import restmock.http.HttpMethod;

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
