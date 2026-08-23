package org.simulatest.restmock.internal.http;

import com.sun.net.httpserver.Headers;

import org.simulatest.restmock.HttpMethod;

/**
 * Cross-origin headers for browser-driven tests.
 *
 * Nothing is emitted unless the caller sent an {@code Origin}, so a plain JVM
 * HTTP client sees a clean response with no CORS noise.
 */
final class Cors {

	private static final String MAX_AGE_SECONDS = "360";

	private Cors() { }

	/** A browser preflight: OPTIONS announcing the method the real request will use. */
	static boolean isPreflight(HttpMethod method, Headers requestHeaders) {
		return method == HttpMethod.OPTIONS
			&& requestHeaders.getFirst(HttpHeader.ORIGIN) != null
			&& requestHeaders.getFirst(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD) != null;
	}

	/**
	 * Echoes the caller's origin rather than sending {@code *}. A wildcard origin
	 * and {@code Access-Control-Allow-Credentials: true} are mutually exclusive
	 * under the CORS spec, and browsers reject the pair outright, so sending both
	 * (as this used to) failed every credentialed request.
	 *
	 * Applied to error responses too: a 404 with no CORS headers reaches the
	 * browser as an opaque cross-origin failure, which hides the actual status
	 * from whoever is debugging the test.
	 */
	static void apply(Headers requestHeaders, Headers responseHeaders) {
		String origin = requestHeaders.getFirst(HttpHeader.ORIGIN);
		if (origin == null) return;

		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		responseHeaders.set(HttpHeader.VARY, HttpHeader.ORIGIN);
	}

	/**
	 * Answers the preflight. Allowed methods come from the routes actually
	 * registered for the path, and allowed headers mirror whatever the browser
	 * asked for - a fixed list cannot know that this particular test posts JSON
	 * with a bearer token.
	 */
	static void applyPreflight(Headers requestHeaders, Headers responseHeaders, String allowedMethods) {
		apply(requestHeaders, responseHeaders);

		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_MAX_AGE, MAX_AGE_SECONDS);

		String requested = requestHeaders.getFirst(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS);
		if (requested != null) responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, requested);
	}

}
