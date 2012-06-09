package restmock;

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

import restmock.mock.Developer;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.TextPlain;
import restmock.response.XML;

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
	
	@Test
	public void requestXML() throws Exception {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		subject.whenGet("/test/").thenReturn(new XML(simpleXML));
		subject.start();
		
		requestGetWithResultString(simpleXML);
	}
	
	@Test
	public void requestXMLObject() throws Exception {
		Developer developerMock = new Developer("Bob", 25);
		subject.whenGet("/test/").thenReturn(new XML(developerMock));
		subject.start();
		
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		
		requestGetWithResultString(simpleXML);
	}


	@Test
	public void requestPlainTextGetWithParameters() throws Exception {
		subject.whenGet("/test/").thenReturn(new TextPlain("Hello ${name}!"));
		subject.start();

		requestGetWithResultString(baseUrl + "/test/?name=Bob", "Hello Bob!");
	}
	
	@Test
	public void requestPlainTextGetWithManyParameters() throws Exception {
		subject.whenGet("/test/").thenReturn(new TextPlain("Hello ${name}, you are the number #${number}!"));
		subject.start();

		requestGetWithResultString(baseUrl + "/test/?name=Bob&number=1", "Hello Bob, you are the number #1!");
	}
	
	
	@Test
	public void postWithoutParametersWithPlainTextResponse() throws Exception {
		subject.whenPost("/test/").thenReturn(new TextPlain("Post succeed"));
		subject.start();
		
		requestMethodWithResultString("Post succeed", HttpMethods.POST);
	}
	
	private void requestMethodWithResultString(String expectedBody, String method) throws Exception {
		requestMethodWithResultString(baseUrl + "/test/", expectedBody, method);		
	}

	private void requestGetWithResultString(String url, String expectedBody) throws Exception {
		requestMethodWithResultString(url, expectedBody, HttpMethods.GET);
	}

	private void requestGetWithResultString(String expectedBody) throws Exception {
		requestMethodWithResultString(baseUrl + "/test/", expectedBody, HttpMethods.GET);		
	}
	
	private void requestMethodWithResultString(String url, String expectedBody, String method) throws IOException, InterruptedException,
			UnsupportedEncodingException {
		ContentExchange exchange = new ContentExchange(false);
		exchange.setURL(url);
		exchange.setMethod(method);
		
		client.send(exchange);
		
		int exchangeState = exchange.waitForDone();
		
		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		assertEquals(expectedBody + "\r\n", exchange.getResponseContent());
	}
	
}
