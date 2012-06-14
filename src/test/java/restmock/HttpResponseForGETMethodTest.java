package restmock;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import restmock.mock.Developer;
import restmock.request.HttpMethod;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.request.RouteRegister;
import restmock.response.ContentType;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.Response;
import restmock.response.TextPlain;
import restmock.response.XML;

public class HttpResponseForGETMethodTest {

	private RouteRegister subject;
	private Route route;

	@Before
	public void setUp() {
		route = new Route(HttpMethod.GET, "/teste/");
		subject = new RouteRegister(route);
	}

	@Test
	public void testPlainTextResponse() throws Exception {
		subject.thenReturn(new TextPlain("Hello World!"));
		
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Hello World!", response.getContent());
	}

	@Test
	public void testHtmlResponse() throws Exception {
		subject.thenReturn(new Html("<h1>Mock rules</h1>"));
		
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Mock rules</h1>", response.getContent());
	}
	
	@Test
	public void testJSONResponse() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		subject.thenReturn(new JSON(simpleJSON));
		
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(simpleJSON, response.getContent());
	}
	
	@Test
	public void testJSONObjectResponse() throws Exception {
		subject.thenReturn(new JSON(new Developer("Bob", 25)));
		
		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(expectedJSON, response.getContent());
	}
	
	@Test
	public void testXMLStringResponse() {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		subject.thenReturn(new XML(simpleXML));
		
		Response response = RouteManager.getInstance().get(route);
		
		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(simpleXML, response.getContent());
	}
	
	@Test
	public void testXMLObjectResponse() {
		subject.thenReturn(new XML(new Developer("Bob", 25)));
		
		String expectedXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		Response response = RouteManager.getInstance().get(route);
		
		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(expectedXML, response.getContent());		
	}
	
}
