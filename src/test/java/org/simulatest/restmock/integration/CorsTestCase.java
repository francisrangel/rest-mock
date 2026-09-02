package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

public class CorsTestCase extends IntegrationTestBase {

	private static final String ORIGIN = "http://example.com";

	@Test
	public void preflightIsAnsweredForARouteStubbedOnlyForGet() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{\"ok\":true}");

		HttpResponse<String> response = preflight("/api/data", "GET", null);

		assertEquals(204, response.statusCode());
		assertEquals(ORIGIN, header(response, HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(Set.of("GET", "HEAD", "OPTIONS"), methods(response, HttpHeader.ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test
	public void preflightAllowsTheHeadersTheBrowserAsksFor() throws Exception {
		RestMock.whenPost("/api/orders").thenReturnJSON("{\"id\":1}");

		HttpResponse<String> response = preflight("/api/orders", "POST", "content-type, authorization");

		assertEquals(204, response.statusCode());
		assertEquals("content-type, authorization", header(response, HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS));
	}

	/**
	 * A wildcard origin cannot be combined with credentials: browsers reject the
	 * pair, which is what made the previous always-on "*" unusable.
	 */
	@Test
	public void originIsEchoedSoCredentialsAreLegal() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{}");

		HttpResponse<String> response = getWithOrigin("/api/data");

		assertEquals(ORIGIN, header(response, HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("true", header(response, HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpHeader.ORIGIN, header(response, HttpHeader.VARY));
	}

	@Test
	public void errorResponsesCarryCorsHeadersSoTheBrowserSeesTheRealStatus() throws Exception {
		HttpResponse<String> response = getWithOrigin("/never-stubbed");

		assertEquals(404, response.statusCode());
		assertEquals(ORIGIN, header(response, HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void preflightForAnUnstubbedPathIsANotFoundTheBrowserCanRead() throws Exception {
		HttpResponse<String> response = preflight("/never-stubbed", "GET", null);

		assertEquals(404, response.statusCode());
		assertEquals(ORIGIN, header(response, HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void anExplicitOptionsStubStillWins() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{}");
		RestMock.whenOptions("/api/data").thenReturnText("mine");

		HttpResponse<String> response = preflight("/api/data", "GET", null);

		assertEquals(200, response.statusCode());
		assertEquals("mine", response.body());
	}

	/**
	 * Stubbing OPTIONS used to switch the preflight off: the explicit match
	 * skipped the preflight path, so the browser got a 200 with a body but no
	 * Access-Control-Allow-Methods, no Allow-Headers, and no Max-Age. CORS
	 * worked until the route was stubbed and then stopped, which is backwards -
	 * the stub is the more deliberate act, so it must answer at least as well.
	 */
	@Test
	public void anExplicitOptionsStubStillAnswersThePreflight() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{}");
		RestMock.whenOptions("/api/data").thenReturnText("mine");

		HttpResponse<String> response = preflight("/api/data", "GET", "content-type, authorization");

		assertEquals("mine", response.body());
		assertEquals(ORIGIN, header(response, HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(Set.of("GET", "HEAD", "OPTIONS"), methods(response, HttpHeader.ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("content-type, authorization", header(response, HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS));
		assertEquals("360", header(response, HttpHeader.ACCESS_CONTROL_MAX_AGE));
	}

	/** The derived and the stubbed answer advertise the same methods; only the body differs. */
	@Test
	public void aStubbedPreflightAdvertisesWhatTheDerivedOneDoes() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{}");
		RestMock.whenPost("/api/data").thenReturnJSON("{}");

		Set<String> derived = methods(preflight("/api/data", "GET", null), HttpHeader.ACCESS_CONTROL_ALLOW_METHODS);

		RestMock.whenOptions("/api/data").thenReturnText("mine");

		Set<String> stubbed = methods(preflight("/api/data", "GET", null), HttpHeader.ACCESS_CONTROL_ALLOW_METHODS);

		assertEquals(derived, stubbed);
	}

	/**
	 * An OPTIONS without Access-Control-Request-Method is not a preflight, so it
	 * gets the plain CORS headers and none of the preflight ones. Guards the fix
	 * above from being over-applied to every OPTIONS request.
	 */
	@Test
	public void aStubbedOptionsThatIsNotAPreflightGetsNoPreflightHeaders() throws Exception {
		RestMock.whenOptions("/api/data").thenReturnText("mine");

		HttpResponse<String> response = optionsWithOrigin("/api/data");

		assertEquals("mine", response.body());
		assertEquals(ORIGIN, header(response, HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty());
		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_MAX_AGE).isEmpty());
	}

	/** No Origin means no browser, so a stubbed preflight-shaped call still gets a clean response. */
	@Test
	public void aStubbedOptionsWithoutOriginGetsNoCorsHeaders() throws Exception {
		RestMock.whenOptions("/api/data").thenReturnText("mine");

		HttpResponse<String> response = sendRequest("/api/data", HttpMethod.OPTIONS);

		assertEquals("mine", response.body());
		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty());
		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty());
	}

	/** Non-browser clients send no Origin, so they should see no CORS headers at all. */
	@Test
	public void aRequestWithoutOriginGetsNoCorsHeaders() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{}");

		HttpResponse<String> response = sendRequest("/api/data", HttpMethod.GET);

		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty());
		assertTrue(response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS).isEmpty());
	}

	private HttpResponse<String> preflight(String path, String requestMethod, String requestHeaders) throws Exception {
		HttpRequest.Builder request = request(path)
			.header(HttpHeader.ORIGIN, ORIGIN)
			.header(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod);

		if (requestHeaders != null) request.header(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, requestHeaders);

		return client.send(request.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
			HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> getWithOrigin(String path) throws Exception {
		return client.send(
			request(path).header(HttpHeader.ORIGIN, ORIGIN).GET().build(),
			HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> optionsWithOrigin(String path) throws Exception {
		return client.send(
			request(path)
				.header(HttpHeader.ORIGIN, ORIGIN)
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
				.build(),
			HttpResponse.BodyHandlers.ofString());
	}

	private static String header(HttpResponse<String> response, String name) {
		return response.headers().firstValue(name).orElse(null);
	}

	private static Set<String> methods(HttpResponse<String> response, String name) {
		String value = header(response, name);

		assertNotNull(value, () -> "expected a " + name + " header, but the response sent none: " + response.headers().map());

		return Set.of(value.split(",\s*"));
	}

}
