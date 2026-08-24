package org.simulatest.restmock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.simulatest.restmock.internal.response.JSON;
import org.simulatest.restmock.internal.response.XML;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.internal.server.RestMockServer;

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
 * RestMock holds process-wide state: routes, recorded requests, the JSON/XML
 * mappers, and the server itself are static singletons. Do not run RestMock-using
 * tests in parallel within the same JVM (e.g. Surefire's {@code parallel=classes}).
 * {@link RestMockExtension} clears state between tests; if you drive the server
 * manually, call {@link #clean()} between tests.
 */
public final class RestMock {

	/** Port used when no port is specified. */
	public static final int DEFAULT_PORT = 9080;

	private static final RouteManager routeManager = new RouteManager();
	private static final RequestLog requestLog = new RequestLog();
	private static final RestMockServer server = new RestMockServer(routeManager, requestLog::add);

	private RestMock() {}

	/** Stubs a GET response for {@code uri}. Chain a {@code thenReturn*} to set the body. */
	public static RestMockResponse whenGet(String uri) {
		return registerRoute(HttpMethod.GET, uri);
	}

	/** Stubs a POST response for {@code uri}. */
	public static RestMockResponse whenPost(String uri) {
		return registerRoute(HttpMethod.POST, uri);
	}

	/** Stubs a PUT response for {@code uri}. */
	public static RestMockResponse whenPut(String uri) {
		return registerRoute(HttpMethod.PUT, uri);
	}

	/** Stubs a DELETE response for {@code uri}. */
	public static RestMockResponse whenDelete(String uri) {
		return registerRoute(HttpMethod.DELETE, uri);
	}

	/** Stubs a PATCH response for {@code uri}. */
	public static RestMockResponse whenPatch(String uri) {
		return registerRoute(HttpMethod.PATCH, uri);
	}

	/** Stubs a HEAD response for {@code uri}. See class doc for HEAD body semantics. */
	public static RestMockResponse whenHead(String uri) {
		return registerRoute(HttpMethod.HEAD, uri);
	}

	/** Stubs an OPTIONS response for {@code uri}. See class doc for the Allow header behavior. */
	public static RestMockResponse whenOptions(String uri) {
		return registerRoute(HttpMethod.OPTIONS, uri);
	}

	/**
	 * Returns every request the server has received since the last {@link #clean()}.
	 * Use it to assert that the system under test made the calls you expected.
	 */
	public static RequestLog requests() {
		return requestLog;
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

	private static RestMockResponse registerRoute(HttpMethod method, String uri) {
		return new RouteRegister(new Route(method, uri), routeManager);
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
		server.start(port);
	}

	/** The port the server is bound to, or -1 when it is not running. */
	public static int port() {
		return server.port();
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
		int port = port();

		if (port == RestMockServer.NOT_RUNNING)
			throw new IllegalStateException(
				"RestMock is not running, so it has no base URL. Call RestMock.startServer() "
					+ "or register RestMockExtension on a static field.");

		return "http://localhost:" + port;
	}

	/**
	 * {@link #baseUrl()} joined to {@code path}, for example
	 * {@code RestMock.url("/users/42")}. A leading slash is added if missing.
	 */
	public static String url(String path) {
		String base = baseUrl();

		if (path == null || path.isEmpty()) return base;
		return path.startsWith("/") ? base + path : base + "/" + path;
	}

	/** Stops the server and clears all routes and recorded requests. No-op if not running. */
	public static void stopServer() {
		server.stop();
		clean();
	}

	/** Removes all stubbed routes and clears the request log. The server keeps running. */
	public static void clean() {
		routeManager.clean();
		requestLog.clear();
	}

	static RouteManager routeManager() {
		return routeManager;
	}

}
