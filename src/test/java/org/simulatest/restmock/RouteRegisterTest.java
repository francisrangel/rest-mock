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
import org.simulatest.restmock.internal.response.Template;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.mock.Developer;

/** Every {@code thenReturn*} registers the response it promises, whatever the method. */
public class RouteRegisterTest {

	private RouteManager routeManager;
	private RouteRegister subject;
	private Route route;

	@BeforeEach
	public void setUp() {
		routeManager = new RouteManager();
		route = new Route(HttpMethod.GET, "/test");
		subject = new RouteRegister(route, routeManager);
	}

	private Response registered() {
		return routeManager.get(route);
	}

	private Template template() {
		return assertInstanceOf(Template.class, registered());
	}

	@Test
	public void thenReturnTextRegistersAPlainTextResponse() {
		subject.thenReturnText("Hello World!");

		assertEquals(ContentType.TEXT_PLAIN, registered().getContentType());
		assertEquals("Hello World!", template().getContent());
	}

	@Test
	public void theMethodIsPartOfTheRoute() {
		Route post = new Route(HttpMethod.POST, "/test");
		new RouteRegister(post, routeManager).thenReturnText("Test succeed");

		assertEquals("Test succeed", assertInstanceOf(Template.class, routeManager.get(post)).getContent());
		assertEquals(NotConfigured.class, registered().getClass(), "the GET route must be untouched");
	}

	@Test
	public void thenReturnHTMLRegistersAnHtmlResponse() {
		subject.thenReturnHTML("<h1>Mock rules</h1>");

		assertEquals(ContentType.TEXT_HTML, registered().getContentType());
		assertEquals("<h1>Mock rules</h1>", template().getContent());
	}

	@Test
	public void thenReturnJSONWithAStringKeepsItVerbatim() {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		subject.thenReturnJSON(simpleJSON);

		assertEquals(ContentType.APPLICATION_JSON, registered().getContentType());
		assertEquals(simpleJSON, template().getContent());
	}

	@Test
	public void thenReturnJSONWithAnObjectSerializesIt() {
		subject.thenReturnJSON(new Developer("Bob", 25));

		assertEquals(ContentType.APPLICATION_JSON, registered().getContentType());
		assertEquals("{\"name\":\"Bob\",\"age\":25}", template().getContent());
	}

	@Test
	public void thenReturnXMLWithAStringKeepsItVerbatim() {
		String simpleXML = "<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>";
		subject.thenReturnXML(simpleXML);

		assertEquals(ContentType.TEXT_XML, registered().getContentType());
		assertEquals(simpleXML, template().getContent());
	}

	@Test
	public void thenReturnXMLWithAnObjectSerializesIt() {
		subject.thenReturnXML(new Developer("Bob", 25));

		assertEquals(ContentType.TEXT_XML, registered().getContentType());
		assertEquals("<Developer><name>Bob</name><age>25</age></Developer>", template().getContent());
	}

	@Test
	public void withHeaderAttachesTheHeaderToTheResponse() {
		subject.thenReturnText("ok").withHeader("Cache-Control", "no-cache");

		assertEquals("no-cache", registered().getHeaders().get("Cache-Control"));
	}

	@Test
	public void lastValueWinsForARepeatedHeader() {
		subject.thenReturnText("ok").withHeader("X-Retry", "1").withHeader("X-Retry", "2");

		assertEquals("2", registered().getHeaders().get("X-Retry"));
		assertEquals(1, registered().getHeaders().size());
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

		assertEquals(ContentType.APPLICATION_JSON, registered().getContentType());
		assertEquals("{\"name\":\"Bob\",\"age\":25}", template().getContent());
	}

	@Test
	public void thenReturnXMLFromResourceLoadsTheFile() {
		subject.thenReturnXMLFromResource("developer.xml");

		assertEquals(ContentType.TEXT_XML, registered().getContentType());
		assertEquals("<?xml version=\"1.0\" ?><developer><name>Bob</name><age>25</age></developer>",
			template().getContent());
	}

	@Test
	public void thenReturnHTMLFromResourceLoadsTheFile() {
		subject.thenReturnHTMLFromResource("page.html");

		assertEquals(ContentType.TEXT_HTML, registered().getContentType());
		assertEquals("<h1>Hello</h1>", template().getContent());
	}

	@Test
	public void thenReturnTextFromResourceLoadsTheFile() {
		subject.thenReturnTextFromResource("example.txt");

		assertEquals(ContentType.TEXT_PLAIN, registered().getContentType());
		assertEquals("rest-mock rock! :-)", template().getContent());
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
		assertEquals("real response", assertInstanceOf(Template.class, response).getContent());
		assertEquals(200, response.getResponseStatus());
	}

	@Test
	public void withStatusSetsCustomStatusCode() {
		subject.thenReturnJSON("{\"id\":1}").withStatus(201);

		assertEquals(ContentType.APPLICATION_JSON, registered().getContentType());
		assertEquals(201, registered().getResponseStatus());
	}

	@Test
	public void withStatusChainsWithHeader() {
		subject.thenReturnJSON("{\"error\":\"bad\"}").withStatus(422).withHeader("X-Reason", "validation");

		assertEquals(422, registered().getResponseStatus());
		assertEquals("validation", registered().getHeaders().get("X-Reason"));
	}

	@Test
	public void withDelaySetsDelayOnResponse() {
		subject.thenReturnText("ok").withDelay(500);

		assertEquals(500, registered().getDelayMillis());
	}

	@Test
	public void defaultDelayIsZero() {
		subject.thenReturnText("ok");

		assertEquals(0, registered().getDelayMillis());
	}

	@Test
	public void thenReturnFileWithBytesUsesOctetStreamByDefault() {
		byte[] bytes = {1, 2, 3};
		subject.thenReturnFile(bytes);

		Binary response = assertInstanceOf(Binary.class, registered());

		assertArrayEquals(bytes, response.render(Map.of()));
		assertEquals(ContentType.APPLICATION_OCTET_STREAM, response.getContentType());
	}

	@Test
	public void thenReturnFileWithExplicitContentType() {
		subject.thenReturnFile(new byte[] {1, 2, 3}, "application/x-protobuf");

		assertEquals("application/x-protobuf", registered().getContentType().type());
	}

	@Test
	public void thenReturnFileFromResourceInfersContentType() {
		subject.thenReturnFileFromResource("page.html");

		assertInstanceOf(Binary.class, registered());
		assertEquals(ContentType.TEXT_HTML, registered().getContentType());
	}

	@Test
	public void thenReturnFileFromResourceFallsBackToOctetStreamForUnknownExtension() {
		subject.thenReturnFileFromResource("fixture.xyz");

		assertEquals(ContentType.APPLICATION_OCTET_STREAM, registered().getContentType());
	}

	@Test
	public void thenReturnFileFromResourceWithExplicitContentType() {
		subject.thenReturnFileFromResource("page.html", "application/octet-stream");

		assertEquals(ContentType.APPLICATION_OCTET_STREAM, registered().getContentType());
	}

	@Test
	public void withDelayChainsWithStatusAndHeader() {
		subject.thenReturnJSON("{}")
			.withStatus(201)
			.withDelay(100)
			.withHeader("X-Slow", "yes");

		assertEquals(201, registered().getResponseStatus());
		assertEquals(100, registered().getDelayMillis());
		assertEquals("yes", registered().getHeaders().get("X-Slow"));
	}

}
