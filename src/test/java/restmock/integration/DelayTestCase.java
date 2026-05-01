package restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import restmock.RestMock;
import restmock.http.HttpMethod;

public class DelayTestCase extends IntegrationTestBase {

	@Test
	public void delayedResponseTakesAtLeastTheConfiguredTime() throws Exception {
		RestMock.whenGet("/slow").thenReturnText("done").withDelay(300);

		long start = System.currentTimeMillis();
		HttpResponse<String> response = sendRequest(baseUrl + "/slow", HttpMethod.GET);
		long elapsed = System.currentTimeMillis() - start;

		assertEquals(200, response.statusCode());
		assertTrue(elapsed >= 300, "Expected at least 300ms but took " + elapsed + "ms");
	}

	@Test
	public void noDelayRespondsImmediately() throws Exception {
		RestMock.whenGet("/fast").thenReturnText("done");

		long start = System.currentTimeMillis();
		HttpResponse<String> response = sendRequest(baseUrl + "/fast", HttpMethod.GET);
		long elapsed = System.currentTimeMillis() - start;

		assertEquals(200, response.statusCode());
		assertTrue(elapsed < 200, "Expected fast response but took " + elapsed + "ms");
	}

}
