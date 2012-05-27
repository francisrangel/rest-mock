package br.com.frs.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpMethods;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RestServerTest {

	int port = 8080;
	String baseUrl = "http://localhost:" + port;
	private RestServer subject;
	private HttpClient client;

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

	@Test
	public void helloWorldTest() throws Exception {
		subject.when("/test").thenReturn("Hello World!");
		subject.start();

		requestGetWithResultString("Hello World!\r\n");
	}

	@Test
	public void requestDifferentText() throws Exception {
		subject.when("/test").thenReturn("Mock rules");
		subject.start();

		requestGetWithResultString("Mock rules\r\n");
	}

	private void requestGetWithResultString(String expectedText) throws Exception, IOException, InterruptedException,
			UnsupportedEncodingException {

		ContentExchange exchange = new ContentExchange(false);
		exchange.setURL(baseUrl + "/test/");
		exchange.setMethod(HttpMethods.GET);
		exchange.setRequestContentType("text/html;charset=utf-8");

		client.send(exchange);

		int exchangeState = exchange.waitForDone();

		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		assertEquals(expectedText, exchange.getResponseContent());
	}
}
