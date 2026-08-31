package org.simulatest.restmock.internal;

/**
 * The names a response template can reference as {@code ${...}}.
 *
 * Shared by the extractor that fills the namespace and the renderer that reports
 * what was in it, so the prefix cannot drift between the two.
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

}
