package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.internal.response.Binary;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.NotConfigured;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.mock.Developer;

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
	public void thenReturnTextRegistersAPlainTextResponse() throws Exception {
		subject.thenReturnText("Hello World!");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Hello World!", response.getContent());
	}

	@Test
	public void thenReturnHTMLRegistersAnHtmlResponse() throws Exception {
		subject.thenReturnHTML("<h1>Mock rules</h1>");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Mock rules</h1>", response.getContent());
	}

	@Test
	public void thenReturnJSONWithAStringKeepsItVerbatim() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		subject.thenReturnJSON(simpleJSON);

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(simpleJSON, response.getContent());
	}

	@Test
	public void thenReturnJSONWithAnObjectSerializesIt() throws Exception {
		subject.thenReturnJSON(new Developer("Bob", 25));

		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";
		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(expectedJSON, response.getContent());
	}

	@Test
	public void thenReturnXMLWithAStringKeepsItVerbatim() {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		subject.thenReturnXML(simpleXML);

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(simpleXML, response.getContent());
	}

	@Test
	public void thenReturnXMLWithAnObjectSerializesIt() {
		subject.thenReturnXML(new Developer("Bob", 25));

		String expectedXML = "<Developer><name>Bob</name><age>25</age></Developer>";
		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals(expectedXML, response.getContent());
	}

	@Test
	public void withHeaderAttachesTheHeaderToTheResponse() {
		subject.thenReturnText("ok").withHeader("Cache-Control", "no-cache");

		Response response = routeManager.get(route);

		assertEquals("no-cache", response.getHeader().get("Cache-Control"));
	}

	@Test
	public void lastValueWinsForARepeatedHeader() {
		subject.thenReturnText("ok").withHeader("X-Retry", "1").withHeader("X-Retry", "2");

		Response response = routeManager.get(route);

		assertEquals("2", response.getHeader().get("X-Retry"));
		assertEquals(1, response.getHeader().size());
	}

	@Test
	public void thenReturnJSONWrapsASerializationFailure() {
		assertThrows(UncheckedIOException.class, () -> subject.thenReturnJSON(new Object() { }));
	}

	/**
	 * No checked exception, so stubbing from a fixture does not force
	 * "throws Exception" onto the test signature.
	 */
	@Test
	public void aMissingResourceFailsUnchecked() {
		UncheckedIOException failure =
			assertThrows(UncheckedIOException.class, () -> subject.thenReturnJSONFromResource("nope.json"));

		assertInstanceOf(FileNotFoundException.class, failure.getCause());
	}

	@Test
	public void thenReturnJSONFromResourceLoadsTheFile() {
		subject.thenReturnJSONFromResource("developer.json");

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals("{\"name\":\"Bob\",\"age\":25}", response.getContent());
	}

	@Test
	public void thenReturnXMLFromResourceLoadsTheFile() {
		subject.thenReturnXMLFromResource("developer.xml");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_XML, response.getContentType());
		assertEquals("<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>",
			response.getContent());
	}

	@Test
	public void thenReturnHTMLFromResourceLoadsTheFile() {
		subject.thenReturnHTMLFromResource("page.html");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Hello</h1>", response.getContent());
	}

	@Test
	public void thenReturnTextFromResourceLoadsTheFile() {
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

	@Test
	public void withStatusSetsCustomStatusCode() {
		subject.thenReturnJSON("{\"id\":1}").withStatus(201);

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(201, response.getResponseStatus());
	}

	@Test
	public void withStatusChainsWithHeader() {
		subject.thenReturnJSON("{\"error\":\"bad\"}").withStatus(422).withHeader("X-Reason", "validation");

		Response response = routeManager.get(route);

		assertEquals(422, response.getResponseStatus());
		assertEquals("validation", response.getHeader().get("X-Reason"));
	}

	@Test
	public void withDelaySetsDelayOnResponse() {
		subject.thenReturnText("ok").withDelay(500);

		Response response = routeManager.get(route);

		assertEquals(500, response.getDelayMillis());
	}

	@Test
	public void defaultDelayIsZero() {
		subject.thenReturnText("ok");

		Response response = routeManager.get(route);

		assertEquals(0, response.getDelayMillis());
	}

	@Test
	public void thenReturnFileWithBytesUsesOctetStreamByDefault() {
		byte[] bytes = {1, 2, 3};
		subject.thenReturnFile(bytes);

		Binary response = assertInstanceOf(Binary.class, routeManager.get(route));

		assertArrayEquals(bytes, response.render(Map.of()));
		assertEquals(ContentType.APPLICATION_OCTET_STREAM, response.getContentType());
	}

	@Test
	public void thenReturnFileWithExplicitContentType() {
		byte[] bytes = {1, 2, 3};
		subject.thenReturnFile(bytes, "application/x-protobuf");

		Response response = routeManager.get(route);

		assertEquals("application/x-protobuf", response.getContentType().type());
	}

	@Test
	public void thenReturnFileFromResourceInfersContentType() {
		subject.thenReturnFileFromResource("page.html");

		Response response = assertInstanceOf(Binary.class, routeManager.get(route));

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
	}

	@Test
	public void thenReturnFileFromResourceFallsBackToOctetStreamForUnknownExtension() throws Exception {
		subject.thenReturnFileFromResource("fixture.xyz");

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_OCTET_STREAM, response.getContentType());
	}

	@Test
	public void thenReturnFileFromResourceWithExplicitContentType() throws Exception {
		subject.thenReturnFileFromResource("page.html", "application/octet-stream");

		Response response = routeManager.get(route);

		assertEquals(ContentType.APPLICATION_OCTET_STREAM, response.getContentType());
	}

	@Test
	public void withDelayChainsWithStatusAndHeader() {
		subject.thenReturnJSON("{}")
			.withStatus(201)
			.withDelay(100)
			.withHeader("X-Slow", "yes");

		Response response = routeManager.get(route);

		assertEquals(201, response.getResponseStatus());
		assertEquals(100, response.getDelayMillis());
		assertEquals("yes", response.getHeader().get("X-Slow"));
	}

}
