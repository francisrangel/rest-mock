package restmock.integration;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.junit.After;
import org.junit.Before;

import restmock.RestServer;

public class IntegrationTestBase {

	private final int port = 8080;
	protected final String baseUrl = "http://localhost:" + port;

	protected RestServer subject;
	protected HttpClient client;

	@Before
	public void setUp() throws Exception {
		client = new HttpClient();
		client.start();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

		subject = new RestServer(port);
	}

	@After
	public void cleanUp() throws Exception {
		client.stop();
		subject.stop();
	}

	protected void requestMethodWithResultString(String url, String expectedBody, String method) throws Exception {
		ContentExchange exchange = new ContentExchange(false);
		exchange.setURL(url);
		exchange.setMethod(method);

		client.send(exchange);

		int exchangeState = exchange.waitForDone();

		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		assertEquals(expectedBody + "\r\n", exchange.getResponseContent());
	}

}
