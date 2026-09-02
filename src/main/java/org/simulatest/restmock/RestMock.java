package org.simulatest.restmock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.simulatest.restmock.internal.response.JSON;
import org.simulatest.restmock.internal.response.XML;

/**
 * Entry point for stubbing HTTP responses in tests.
 *
 * Typical use from a JUnit test:
 *
 *   {@code @RegisterExtension static RestMockExtension mock = new RestMockExtension();}
 *
 *   RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Ada\"}");
 *   // ... exercise system under test ...
 *   assertEquals(1, RestMock.requests().countForRoute(HttpMethod.GET, "/users/1"));
 *
 * The server runs in-process on {@link #DEFAULT_PORT} (9080) unless you pass
 * a different port to {@link RestMockExtension} or {@link #startServer(int)}.
 *
 * Path templates use brace placeholders (for example {@code /users/{id}}) and
 * captured values are exposed to the response body via {@code ${id}}.
 *
 * A stub URI is a path and nothing else. A query string, a fragment, a missing
 * leading slash, or a malformed placeholder is rejected by the {@code when*}
 * call with an {@link IllegalArgumentException}, rather than compiling into a
 * route no request can reach. A request that matches no route gets a 404 whose
 * body names what is stubbed and how close the call came.
 *
 * {@link #baseUrl()} and {@link #url(String)} build the address of the running
 * server, so a test never has to concatenate a port into a string.
 *
 * HEAD requests reply with the configured body's byte length as Content-Length
 * but no body. OPTIONS requests get an Allow header listing every method
 * registered for that path.
 *
 * Every method here delegates to one default {@link HttpMock}, returned by
 * {@link #defaultMock()}. That instance is process-wide, so tests sharing it
 * must not run in parallel within the same JVM (e.g. Surefire's
 * {@code parallel=classes}). {@link RestMockExtension} clears it between tests;
 * if you drive the server manually, call {@link #clean()} between tests.
 *
 * When you do need more than one mock at a time - parallel classes, or two
 * collaborating services stubbed at once - construct {@link HttpMock} directly
 * and hand it to {@link RestMockExtension}. Instances share nothing but the
 * Jackson mappers behind {@link #json()} and {@link #xml()}, which stay global
 * because Jackson configuration is global by nature.
 */
public final class RestMock {

	/** Port used when no port is specified. */
	public static final int DEFAULT_PORT = 9080;

	private static final HttpMock DEFAULT = new HttpMock();

	private RestMock() {}

	/**
	 * The instance every static method here delegates to.
	 *
	 * Use it to pass the default mock to code that takes an {@link HttpMock}, or
	 * as the starting point for moving a suite off the static API.
	 */
	public static HttpMock defaultMock() {
		return DEFAULT;
	}

	/** Stubs a GET response for {@code uri}. Chain a {@code thenReturn*} to set the body. */
	public static RestMockResponse whenGet(String uri) {
		return DEFAULT.whenGet(uri);
	}

	/** Stubs a POST response for {@code uri}. */
	public static RestMockResponse whenPost(String uri) {
		return DEFAULT.whenPost(uri);
	}

	/** Stubs a PUT response for {@code uri}. */
	public static RestMockResponse whenPut(String uri) {
		return DEFAULT.whenPut(uri);
	}

	/** Stubs a DELETE response for {@code uri}. */
	public static RestMockResponse whenDelete(String uri) {
		return DEFAULT.whenDelete(uri);
	}

	/** Stubs a PATCH response for {@code uri}. */
	public static RestMockResponse whenPatch(String uri) {
		return DEFAULT.whenPatch(uri);
	}

	/** Stubs a HEAD response for {@code uri}. See class doc for HEAD body semantics. */
	public static RestMockResponse whenHead(String uri) {
		return DEFAULT.whenHead(uri);
	}

	/** Stubs an OPTIONS response for {@code uri}. See class doc for the Allow header behavior. */
	public static RestMockResponse whenOptions(String uri) {
		return DEFAULT.whenOptions(uri);
	}

	/**
	 * Returns every request the server has received since the last {@link #clean()}.
	 * Use it to assert that the system under test made the calls you expected.
	 */
	public static RequestLog requests() {
		return DEFAULT.requests();
	}

	/**
	 * Jackson mapper used to serialize objects passed to {@code thenReturnJSON(Object)}.
	 * Returns a shared singleton; modules and configuration registered here persist
	 * for the lifetime of the JVM and are visible to every test.
	 */
	public static ObjectMapper json() {
		return JSON.MAPPER;
	}

	/**
	 * Jackson XML mapper used to serialize objects passed to {@code thenReturnXML(Object)}.
	 * Returns a shared singleton; see {@link #json()} for the lifetime caveat.
	 */
	public static XmlMapper xml() {
		return XML.MAPPER;
	}

	/** Starts the server on {@link #DEFAULT_PORT}. No-op if already running. */
	public static void startServer() {
		startServer(DEFAULT_PORT);
	}

	/**
	 * Starts the server on the given port. Pass 0 to let the OS pick a free one
	 * and read it back from {@link #port()}, which is how you keep parallel CI
	 * jobs on one machine from fighting over {@link #DEFAULT_PORT}.
	 *
	 * No-op if the server is already running on that port. Throws
	 * {@link IllegalStateException} if it is running on a different one, and
	 * {@link java.io.UncheckedIOException} if it cannot bind (port in use, no
	 * permission, etc.).
	 */
	public static void startServer(int port) {
		DEFAULT.startServer(port);
	}

	/** The port the server is bound to, or -1 when it is not running. */
	public static int port() {
		return DEFAULT.port();
	}

	/**
	 * Where the server is listening, for example {@code http://localhost:9080}.
	 * Saves every caller from writing {@code "http://localhost:" + RestMock.port()},
	 * which is the only line of plumbing a test using this library still needs.
	 *
	 * Throws {@link IllegalStateException} when the server is not running: there
	 * is no honest URL to return, and a placeholder would fail later as a
	 * connection refused with nothing pointing back here.
	 */
	public static String baseUrl() {
		return DEFAULT.baseUrl();
	}

	/**
	 * {@link #baseUrl()} joined to {@code path}, for example
	 * {@code RestMock.url("/users/42")}. A leading slash is added if missing.
	 */
	public static String url(String path) {
		return DEFAULT.url(path);
	}

	/** Stops the server and clears all routes and recorded requests. No-op if not running. */
	public static void stopServer() {
		DEFAULT.stopServer();
	}

	/** Removes all stubbed routes and clears the request log. The server keeps running. */
	public static void clean() {
		DEFAULT.clean();
	}

}
