package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

/** A response with nothing in it: the usual answer to a DELETE or a PUT. */
public class NoContentTestCase extends IntegrationTestBase {

	/** The JDK server's own logger; it complains here when it is handed a length it has to correct. */
	private static final Logger SERVER_LOG = Logger.getLogger("com.sun.net.httpserver");

	private final ByteArrayOutputStream logged = new ByteArrayOutputStream();
	private final StreamHandler warnings = new StreamHandler(logged, new SimpleFormatter());

	@BeforeEach
	public void collectServerWarnings() {
		warnings.setLevel(Level.WARNING);
		SERVER_LOG.addHandler(warnings);
	}

	@AfterEach
	public void stopCollecting() {
		SERVER_LOG.removeHandler(warnings);
	}

	@Test
	public void a204RaisesNoServerWarning() throws Exception {
		RestMock.whenDelete("/users/1").thenReturnText("").withStatus(204);

		HttpResponse<String> response = sendRequest("/users/1", HttpMethod.DELETE);

		assertEquals(204, response.statusCode());
		assertEquals("", serverWarnings());
	}

	@Test
	public void a304RaisesNoServerWarning() throws Exception {
		RestMock.whenGet("/cached").thenReturnText("").withStatus(304);

		HttpResponse<String> response = sendRequest("/cached", HttpMethod.GET);

		assertEquals(304, response.statusCode());
		assertEquals("", serverWarnings());
	}

	/** A 204 cannot carry a body, so one stubbed by mistake is dropped rather than complained about per request. */
	@Test
	public void a204DropsABodyStubbedWithIt() throws Exception {
		RestMock.whenDelete("/users/1").thenReturnErrorCodeWithMessage(204, "No Content");

		HttpResponse<String> response = sendRequest("/users/1", HttpMethod.DELETE);

		assertEquals(204, response.statusCode());
		assertEquals("", response.body());
		assertEquals("", serverWarnings());
	}

	@Test
	public void anEmptyBodyDeclaresALengthOfZero() throws Exception {
		RestMock.whenPost("/orders").thenReturnText("");

		HttpResponse<String> response = sendRequest("/orders", HttpMethod.POST);

		assertEquals(200, response.statusCode());
		assertEquals("0", response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElse(null));
		assertTrue(response.headers().firstValue("Transfer-Encoding").isEmpty());
	}

	private String serverWarnings() {
		warnings.flush();
		return logged.toString();
	}

}
