package restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static restmock.utils.StringUtils.singleLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import restmock.mock.Developer;
import restmock.http.HttpMethod;
import restmock.routing.Route;
import restmock.routing.RouteManager;
import restmock.routing.RouteRegister;
import restmock.response.ContentType;
import restmock.response.NotConfigured;
import restmock.response.Response;

public class HttpResponseForGETMethodTest {

	private RouteManager routeManager;
	private RouteRegister subject;
	private Route route;

	@BeforeEach
	public void setUp() {
		routeManager = new RouteManager();
		route = new Route(HttpMethod.GET, "/test");
		subject = new RouteRegister(route, routeManager);
	}

	@Test
	public void testPlainTextResponse() throws Exception {
		subject.thenReturnText("Hello World!");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Hello World!", response.getContent());
	}

	@Test
	public void testHtmlResponse() throws Exception {
		subject.thenReturnHTML("<h1>Mock rules</h1>");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Mock rules</h1>", response.getContent());
	}

	@Test
	public void testJSONResponse() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		subject.thenReturnJSON(simpleJSON);

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(simpleJSON, response.getContent());
	}

	@Test
	public void testJSONObjectResponse() throws Exception {
		subject.thenReturnJSON(new Developer("Bob", 25));

		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";
		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(singleLine(expectedJSON), singleLine(response.getContent()));
	}

	@Test
	public void testXMLStringResponse() {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		subject.thenReturnXML(simpleXML);

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(simpleXML, response.getContent());
	}

	@Test
	public void testXMLObjectResponse() {
		subject.thenReturnXML(new Developer("Bob", 25));

		String expectedXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(expectedXML, response.getContent());
	}

	@Test
	public void testHeaderResponse() {
		subject.thenReturnXML(new Developer("Bob", 25)).withHeader("Cache-Control", "no-cache");
		Response response = routeManager.get(route);

		assertEquals("no-cache", response.getHeader().get("Cache-Control"));
	}

	@Test
	public void testJSONFromResource() throws Exception {
		subject.thenReturnJSONFromResource("developer.json");

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals("{\"name\":\"Bob\",\"age\":25}", response.getContent());
	}

	@Test
	public void testXMLFromResource() throws Exception {
		subject.thenReturnXMLFromResource("developer.xml");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(singleLine("<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>"),
			singleLine(response.getContent()));
	}

	@Test
	public void testHTMLFromResource() throws Exception {
		subject.thenReturnHTMLFromResource("page.html");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Hello</h1>", response.getContent());
	}

	@Test
	public void testTextFromResource() throws Exception {
		subject.thenReturnTextFromResource("example.txt");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("rest-mock rock! :-)", response.getContent());
	}

	@Test
	public void danglingRouteRegistersSentinel() {
		Route dangling = new Route(HttpMethod.GET, "/dangling");
		new RouteRegister(dangling, routeManager);

		Response response = routeManager.get(dangling);

		assertEquals(NotConfigured.class, response.getClass());
		assertEquals(501, response.getResponseStatus());
	}

	@Test
	public void thenReturnReplacesSentinel() {
		Route replaced = new Route(HttpMethod.GET, "/replaced");
		new RouteRegister(replaced, routeManager).thenReturnText("real response");

		Response response = routeManager.get(replaced);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("real response", response.getContent());
		assertEquals(200, response.getResponseStatus());
	}

}
