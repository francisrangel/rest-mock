package org.simulatest.restmock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.simulatest.restmock.internal.response.JSON;
import org.simulatest.restmock.internal.response.XML;

/**
 * Entry point for stubbing HTTP responses in tests.
 *
 * The static methods drive one default mock and need nothing else, which
 * suits a throwaway main or a framework hook:
 *
 *   RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Ada\"}");
 *   RestMock.startServer();
 *
 * In a JUnit test, register {@link RestMockExtension} on a static field and
 * stub on that field instead; it starts and cleans the mock for you.
 *
 * Every method here delegates to the {@link HttpMock} that {@link #defaultMock()}
 * returns. That instance is process-wide, so test classes sharing it must run
 * one at a time; construct an {@link HttpMock} of your own when a JVM needs
 * more than one mock. Instances share nothing but the Jackson mappers behind
 * {@link #json()} and {@link #xml()}.
 *
 * What a stub can do, from path templates and {@code ${name}} placeholders to
 * sequenced responses, derived HEAD and OPTIONS answers, CORS and the 404 that
 * names what is stubbed, is covered by the
 * <a href="https://github.com/francisrangel/rest-mock#readme">README</a>.
 */
public final class RestMock {

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

	/** Stubs HEAD explicitly for {@code uri}. Without one, HEAD is answered from the GET stub. */
	public static RestMockResponse whenHead(String uri) {
		return DEFAULT.whenHead(uri);
	}

	/** Stubs OPTIONS explicitly for {@code uri}. Without one, OPTIONS is answered from the methods stubbed there. */
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

	/** Starts the server on an OS-assigned port. No-op if already running. */
	public static void startServer() {
		DEFAULT.startServer();
	}

	/**
	 * Starts the server on the given port; 0 means OS-assigned, as
	 * {@link #startServer()}.
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
	 * Where the server is listening, for example {@code http://localhost:54321}.
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
