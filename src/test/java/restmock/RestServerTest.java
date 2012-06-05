package restmock;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpMethods;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import restmock.mock.Developer;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.TextPlain;

public class RestServerTest {

	private final int port = 8080;
	private final String baseUrl = "http://localhost:" + port;
	
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
	public void requestPlainText() throws Exception {
		subject.whenGet("/test/").thenReturn(new TextPlain("Hello World!"));
		subject.start();

		requestGetWithResultString("Hello World!");
	}

	@Test
	public void requestHtml() throws Exception {
		subject.whenGet("/test/").thenReturn(new Html("<h1>Mock rules</h1>"));
		subject.start();

		requestGetWithResultString("<h1>Mock rules</h1>");
	}
	
	@Test
	public void requestJSON() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		
		subject.whenGet("/test/").thenReturn(new JSON(simpleJSON));
		subject.start();

		requestGetWithResultString(simpleJSON);
	}
	
	@Test
	public void requestJSONObject() throws Exception {
		subject.whenGet("/test/").thenReturn(new JSON(new Developer("Bob", 25)));
		subject.start();
		
		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";

		requestGetWithResultString(expectedJSON);
	}

	private void requestGetWithResultString(String expectedBody) throws Exception {
		ContentExchange exchange = new ContentExchange(false);
		exchange.setURL(baseUrl + "/test/");
		exchange.setMethod(HttpMethods.GET);

		client.send(exchange);

		int exchangeState = exchange.waitForDone();

		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		assertEquals(expectedBody + "\r\n", exchange.getResponseContent());
	}
	
}
