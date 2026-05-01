package restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import restmock.RestMock;
import restmock.http.HttpMethod;

public class DanglingRouteTestCase extends IntegrationTestBase {

	@Test
	public void danglingRouteReturns501WithDiagnosticMessage() throws Exception {
		RestMock.whenGet("/forgot");

		HttpResponse<String> response = sendRequest(baseUrl + "/forgot", HttpMethod.GET);

		assertEquals(501, response.statusCode());
		assertTrue(response.body().contains("/forgot"));
		assertTrue(response.body().contains("no response was configured"));
	}

	@Test
	public void thenReturnReplacesSentinel() throws Exception {
		RestMock.whenGet("/completed").thenReturnText("ok");

		HttpResponse<String> response = sendRequest(baseUrl + "/completed", HttpMethod.GET);

		assertEquals(200, response.statusCode());
	}

}
