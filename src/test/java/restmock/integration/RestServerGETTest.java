package restmock.integration;

import org.eclipse.jetty.http.HttpMethods;
import org.junit.Test;

import restmock.RestMock;
import restmock.mock.Developer;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.TextPlain;
import restmock.response.XML;

public class RestServerGETTest extends IntegrationTestBase {

	@Test
	public void requestPlainText() throws Exception {
		RestMock.whenGet("/test/").thenReturn(new TextPlain("Hello World!"));
		RestMock.startServer();

		requestGetWithResultString("Hello World!");
	}

	@Test
	public void requestHtml() throws Exception {
		RestMock.whenGet("/test/").thenReturn(new Html("<h1>Mock rules</h1>"));
		RestMock.startServer();

		requestGetWithResultString("<h1>Mock rules</h1>");
	}

	@Test
	public void requestJSON() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";

		RestMock.whenGet("/test/").thenReturn(new JSON(simpleJSON));
		RestMock.startServer();

		requestGetWithResultString(simpleJSON);
	}

	@Test
	public void requestJSONObject() throws Exception {
		RestMock.whenGet("/test/").thenReturn(new JSON(new Developer("Bob", 25)));
		RestMock.startServer();

		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";

		requestGetWithResultString(expectedJSON);
	}

	@Test
	public void requestXML() throws Exception {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		RestMock.whenGet("/test/").thenReturn(new XML(simpleXML));
		RestMock.startServer();

		requestGetWithResultString(simpleXML);
	}

	@Test
	public void requestXMLObject() throws Exception {
		Developer developerMock = new Developer("Bob", 25);
		RestMock.whenGet("/test/").thenReturn(new XML(developerMock));
		RestMock.startServer();

		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";

		requestGetWithResultString(simpleXML);
	}

	@Test
	public void requestPlainTextGetWithParameters() throws Exception {
		RestMock.whenGet("/test/").thenReturn(new TextPlain("Hello ${name}!"));
		RestMock.startServer();

		requestGetWithResultString(baseUrl + "/test/?name=Bob", "Hello Bob!");
	}

	@Test
	public void requestPlainTextGetWithManyParameters() throws Exception {
		RestMock.whenGet("/test/").thenReturn(new TextPlain("Hello ${name}, you are the number #${number}!"));
		RestMock.startServer();

		requestGetWithResultString(baseUrl + "/test/?name=Bob&number=1", "Hello Bob, you are the number #1!");
	}

	private void requestGetWithResultString(String url, String expectedBody) throws Exception {
		requestMethodWithResultString(url, expectedBody, HttpMethods.GET);
	}

	private void requestGetWithResultString(String expectedBody) throws Exception {
		requestMethodWithResultString(baseUrl + "/test/", expectedBody, HttpMethods.GET);
	}

}
