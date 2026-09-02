package org.simulatest.restmock;

import java.util.Locale;

/** HTTP methods the mock server can stub. */
public enum HttpMethod {

	GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

	/** Case-insensitive lookup. Throws {@link IllegalArgumentException} if {@code method} isn't one of the known names. */
	public static HttpMethod byString(String method) {
		try {
			return valueOf(method.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException unknown) {
			throw new IllegalArgumentException(method + " isn't a HttpMethod");
		}
	}

}
