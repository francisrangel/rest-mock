package restmock.integration;

import org.junit.Test;

import restmock.RestMock;
import restmock.mock.Developer;
import restmock.request.HttpMethod;

public class WhenGetTestCase extends IntegrationTestBase {

	@Test
	public void requestPlainText() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello World!");

		requestGetWithResultString("Hello World!");
	}

	@Test
	public void requestHtml() throws Exception {
		RestMock.whenGet("/test").thenReturnHTML("<h1>Mock rules</h1>");

		requestGetWithResultString("<h1>Mock rules</h1>");
	}

	@Test
	public void requestJSON() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";

		RestMock.whenGet("/test").thenReturnJSON(simpleJSON);

		requestGetWithResultString(simpleJSON);
	}

	@Test
	public void requestJSONObject() throws Exception {
		RestMock.whenGet("/test").thenReturnJSON(new Developer("Bob", 25));

		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";

		requestGetWithResultString(expectedJSON);
	}

	@Test
	public void requestXML() throws Exception {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		RestMock.whenGet("/test").thenReturnXML(simpleXML);

		requestGetWithResultString(simpleXML);
	}

	@Test
	public void requestXMLObject() throws Exception {
		Developer developerMock = new Developer("Bob", 25);
		RestMock.whenGet("/test").thenReturnXML(developerMock);

		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";

		requestGetWithResultString(simpleXML);
	}

	@Test
	public void requestPlainTextGetWithParameters() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello ${name}!");

		requestGetWithResultString(baseUrl + "/test?name=Bob", "Hello Bob!");
	}

	@Test
	public void requestPlainTextGetWithManyParameters() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello ${name}, you are the number #${number}!");

		requestGetWithResultString(baseUrl + "/test?name=Bob&number=1", "Hello Bob, you are the number #1!");
	}

	private void requestGetWithResultString(String url, String expectedBody) throws Exception {
		requestMethodWithResultString(url, expectedBody, HttpMethod.GET);
	}

	private void requestGetWithResultString(String expectedBody) throws Exception {
		requestMethodWithResultString(baseUrl + "/test", expectedBody, HttpMethod.GET);
	}

}
