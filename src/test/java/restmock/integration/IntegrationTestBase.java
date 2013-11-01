package restmock.integration;

import static org.junit.Assert.assertEquals;
import static restmock.utils.Resource.LINE_SEPARATOR;

import java.io.IOException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class IntegrationTestBase {
	
	protected final String baseUrl = "http://localhost:9080";

	protected static HttpClient client = new HttpClient();
	
	@BeforeClass
	public static void setUp() throws Exception {
		client.start();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		
		RestMock.startServer();
	}

	@AfterClass
	public static void cleanUp() throws Exception {
		client.stop();
		RestMock.stopServer();
	}
	
	@After
	public void cleanUpRoutes() {
		RestMock.clean();
	}

	protected void requestMethodWithResultString(String url, String expectedBody, HttpMethod method) throws Exception {
		ContentExchange exchange = sendRequestAndWaitForDone(url, method);
		assertEquals(expectedBody + LINE_SEPARATOR, exchange.getResponseContent());
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
