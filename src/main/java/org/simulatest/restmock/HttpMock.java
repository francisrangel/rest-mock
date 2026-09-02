package org.simulatest.restmock;

import org.simulatest.restmock.internal.http.FrontController;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.internal.server.RestMockServer;

/**
 * One mock HTTP server: its routes, its recorded requests, and its port.
 *
 * Most tests never name this type. {@link RestMock} is a facade over a single
 * default instance, and {@code RestMock.whenGet(...)} is the shorter way to say
 * {@code RestMock.defaultMock().whenGet(...)}.
 *
 * Reach for an instance when one JVM needs more than one mock at a time: two
 * test classes running in parallel, or a test that stubs two collaborating
 * services on separate ports.
 *
 *   {@code HttpMock payments = new HttpMock();}
 *   {@code payments.startServer(0);}
 *   {@code payments.whenGet("/charges/1").thenReturnJSON("{}");}
 *   {@code String url = payments.url("/charges/1");}
 *
 * Instances share nothing except the Jackson mappers behind
 * {@link RestMock#json()} and {@link RestMock#xml()}, which stay process-wide
 * because Jackson configuration is global by nature.
 *
 * An instance is safe to use from several threads: routes may be registered
 * while the server is serving, and the request log takes a snapshot on every
 * read.
 */
public final class HttpMock {

	private final RouteManager routeManager = new RouteManager();
	private final RequestLog requestLog = new RequestLog();
	private final RestMockServer server = new RestMockServer(new FrontController(routeManager, requestLog::add));

	/** Stubs a GET response for {@code uri}. Chain a {@code thenReturn*} to set the body. */
	public RestMockResponse whenGet(String uri) {
		return registerRoute(HttpMethod.GET, uri);
	}

	/** Stubs a POST response for {@code uri}. */
	public RestMockResponse whenPost(String uri) {
		return registerRoute(HttpMethod.POST, uri);
	}

	/** Stubs a PUT response for {@code uri}. */
	public RestMockResponse whenPut(String uri) {
		return registerRoute(HttpMethod.PUT, uri);
	}

	/** Stubs a DELETE response for {@code uri}. */
	public RestMockResponse whenDelete(String uri) {
		return registerRoute(HttpMethod.DELETE, uri);
	}

	/** Stubs a PATCH response for {@code uri}. */
	public RestMockResponse whenPatch(String uri) {
		return registerRoute(HttpMethod.PATCH, uri);
	}

	/** Stubs a HEAD response for {@code uri}. See {@link RestMock} for HEAD body semantics. */
	public RestMockResponse whenHead(String uri) {
		return registerRoute(HttpMethod.HEAD, uri);
	}

	/** Stubs an OPTIONS response for {@code uri}. See {@link RestMock} for the Allow header behavior. */
	public RestMockResponse whenOptions(String uri) {
		return registerRoute(HttpMethod.OPTIONS, uri);
	}

	/**
	 * Every request this mock has received since the last {@link #clean()}.
	 * Use it to assert that the system under test made the calls you expected.
	 */
	public RequestLog requests() {
		return requestLog;
	}

	/** Starts this mock on {@link RestMock#DEFAULT_PORT}. No-op if already running. */
	public void startServer() {
		startServer(RestMock.DEFAULT_PORT);
	}

	/**
	 * Starts this mock on the given port. Pass 0 to let the OS pick a free one
	 * and read it back from {@link #port()}, which is how several mocks coexist
	 * in one JVM without agreeing on port numbers in advance.
	 *
	 * No-op if already running on that port. Throws {@link IllegalStateException}
	 * if it is running on a different one, and {@link java.io.UncheckedIOException}
	 * if it cannot bind.
	 */
	public void startServer(int port) {
		server.start(port);
	}

	/** Stops this mock and clears its routes and recorded requests. No-op if not running. */
	public void stopServer() {
		server.stop();
		clean();
	}

	/** Removes this mock's stubbed routes and clears its request log. The server keeps running. */
	public void clean() {
		routeManager.clean();
		requestLog.clear();
	}

	/** The port this mock is bound to, or -1 when it is not running. */
	public int port() {
		return server.port();
	}

	/**
	 * Where this mock is listening, for example {@code http://localhost:9080}.
	 *
	 * Throws {@link IllegalStateException} when it is not running: there is no
	 * honest URL to return, and a placeholder would fail later as a connection
	 * refused with nothing pointing back here.
	 */
	public String baseUrl() {
		if (!server.isRunning())
			throw new IllegalStateException(
				"This mock is not running, so it has no base URL. Call startServer() "
					+ "or register RestMockExtension on a static field.");

		return "http://localhost:" + port();
	}

	/**
	 * {@link #baseUrl()} joined to {@code path}, for example {@code url("/users/42")}.
	 * A leading slash is added if missing.
	 */
	public String url(String path) {
		String base = baseUrl();

		if (path == null || path.isEmpty()) return base;
		return path.startsWith("/") ? base + path : base + "/" + path;
	}

	private RestMockResponse registerRoute(HttpMethod method, String uri) {
		return new RouteRegister(new Route(method, uri), routeManager);
	}

	RouteManager routeManager() {
		return routeManager;
	}

}
