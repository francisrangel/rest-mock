package restmock;

import static org.junit.Assert.assertEquals;
import static restmock.utils.StringUtils.singleLine;

import org.junit.Before;
import org.junit.Test;

import restmock.mock.Developer;
import restmock.request.HttpMethod;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.request.RouteRegister;
import restmock.response.ContentType;
import restmock.response.Response;

public class HttpResponseForGETMethodTest {

	private RouteRegister subject;
	private Route route;

	@Before
	public void setUp() {
		route = new Route(HttpMethod.GET, "/test");
		subject = new RouteRegister(route);
	}

	@Test
	public void testPlainTextResponse() throws Exception {
		subject.thenReturnText("Hello World!");
		
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Hello World!", response.getContent());
	}

	@Test
	public void testHtmlResponse() throws Exception {
		subject.thenReturnHTML("<h1>Mock rules</h1>");
		
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Mock rules</h1>", response.getContent());
	}
	
	@Test
	public void testJSONResponse() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		subject.thenReturnJSON(simpleJSON);
		
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(simpleJSON, response.getContent());
	}
	
	@Test
	public void testJSONObjectResponse() throws Exception {
		subject.thenReturnJSON(new Developer("Bob", 25));
		
		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";
		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(singleLine(expectedJSON), singleLine(response.getContent()));
	}
	
	@Test
	public void testXMLStringResponse() {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		subject.thenReturnXML(simpleXML);
		
		Response response = RouteManager.getInstance().get(route);
		
		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(simpleXML, response.getContent());
	}
	
	@Test
	public void testXMLObjectResponse() {
		subject.thenReturnXML(new Developer("Bob", 25));
		
		String expectedXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		Response response = RouteManager.getInstance().get(route);
		
		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(expectedXML, response.getContent());		
	}
	
	@Test
	public void testHeaderResponse() {
		subject.thenReturnXML(new Developer("Bob", 25)).withHeader("Cache-Control", "no-cache");
		Response response = RouteManager.getInstance().get(route);
		
		assertEquals("no-cache", response.getHeader().get("Cache-Control"));
	}
	
}
