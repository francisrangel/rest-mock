package org.simulatest.restmock.internal.http;

import java.util.Collection;

import com.sun.net.httpserver.Headers;

/**
 * Cross-origin headers for browser-driven tests.
 *
 * Nothing is emitted unless the caller sent an {@code Origin}, so a plain JVM
 * HTTP client sees a clean response with no CORS noise.
 */
final class Cors {

	private Cors() { }

	/**
	 * A browser preflight: an OPTIONS request announcing the method the real
	 * request will use. Only consulted while answering OPTIONS, so the method
	 * itself is not checked here.
	 */
	static boolean isPreflight(Headers requestHeaders) {
		return requestHeaders.getFirst(HttpHeader.ORIGIN) != null
			&& requestHeaders.getFirst(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD) != null;
	}

	/**
	 * Echoes the caller's origin rather than sending {@code *}. A wildcard origin
	 * and {@code Access-Control-Allow-Credentials: true} are mutually exclusive
	 * under the CORS spec, and browsers reject the pair outright.
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
	 * Lets the page read the headers a stub set. Without this a browser hides
	 * every response header but a safelisted few, so {@code Location} or
	 * {@code X-Total-Count} reads as null in the page while a JVM client sees
	 * it. Named one by one: the wildcard does not count on a credentialed
	 * request.
	 */
	static void expose(Headers requestHeaders, Headers responseHeaders, Collection<String> names) {
		if (requestHeaders.getFirst(HttpHeader.ORIGIN) == null || names.isEmpty()) return;

		responseHeaders.set(HttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", names));
	}

	/**
	 * Answers the preflight, on top of the headers {@link #apply} already sent
	 * for the request. The method and the headers the browser asked for are
	 * mirrored back, whatever is stubbed: a refused preflight reaches the page
	 * as an opaque failure, while the request it lets through gets the 404 that
	 * says what is stubbed. No max-age: stubs change between tests, and a
	 * cached preflight would keep answering for the previous ones.
	 */
	static void applyPreflight(Headers requestHeaders, Headers responseHeaders) {
		responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS,
			requestHeaders.getFirst(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD));

		String requested = requestHeaders.getFirst(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS);
		if (requested != null) responseHeaders.set(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, requested);
	}

}
