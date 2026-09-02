package org.simulatest.restmock.internal;

import java.util.Locale;

/**
 * The names a response template can reference as {@code ${...}}.
 *
 * Shared by the extractor that fills the namespace and the renderer that reads
 * and reports it, so the prefix and the case rule cannot drift between them.
 */
public final class Placeholders {

	/**
	 * Request headers are addressed as {@code ${header.Accept}}, never as a bare
	 * {@code ${Accept}}.
	 *
	 * The bare namespace holds what the stub author wrote - path captures, body
	 * fields, query parameters. Headers are mostly ambient: Host, User-Agent and
	 * Accept are attached by the HTTP client, not by the test. Letting them share
	 * the namespace meant a typo or a missing body field could silently resolve
	 * to one of them, which is the one case the "No value for ${...}" failure
	 * could not catch.
	 */
	public static final String HEADER_PREFIX = "header.";

	private Placeholders() { }

	/** True for a name in the header namespace, matched case-insensitively. */
	public static boolean isHeader(String name) {
		return name.regionMatches(true, 0, HEADER_PREFIX, 0, HEADER_PREFIX.length());
	}

	/**
	 * The key a request header is stored under. Header names fold case because
	 * HTTP says they do, and because the JDK server rewrites {@code X-Tenant}
	 * to {@code X-tenant} before anyone here sees it.
	 */
	public static String headerKey(String headerName) {
		return HEADER_PREFIX + headerName.toLowerCase(Locale.ROOT);
	}

	/**
	 * The key a template name resolves through. Headers fold case as above;
	 * every other name is matched exactly, the same way
	 * {@code ReceivedRequest.queryParam} matches, so {@code ${Name}} does not
	 * quietly find {@code name}.
	 */
	public static String key(String name) {
		return isHeader(name) ? headerKey(name.substring(HEADER_PREFIX.length())) : name;
	}

}
