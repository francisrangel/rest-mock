package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.junit.After;
import org.junit.Before;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class IntegrationTestBase {

	private final int port = 8080;
	protected final String baseUrl = "http://localhost:" + port;

	protected HttpClient client;

	@Before
	public void setUp() throws Exception {
		client = new HttpClient();
		client.start();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
	}

	@After
	public void cleanUp() throws Exception {
		client.stop();
		RestMock.stopServer();
	}

	protected void requestMethodWithResultString(String url, String expectedBody, HttpMethod method) throws Exception {
		ContentExchange exchange = sendRequestAndWaitForDone(url, method);
		assertEquals(expectedBody + "\r\n", exchange.getResponseContent());
	}

	protected ContentExchange sendRequestAndWaitForDone(String url, HttpMethod method) throws IOException, InterruptedException {
		ContentExchange exchange = new ContentExchange(false);
		exchange.setURL(url);
		exchange.setMethod(method.name());

		client.send(exchange);

		int exchangeState = exchange.waitForDone();

		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		
		return exchange;
	}

}
