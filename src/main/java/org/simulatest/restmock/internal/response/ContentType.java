package org.simulatest.restmock.internal.response;

import java.net.URLConnection;

public record ContentType(String type) {

	public static final ContentType TEXT_HTML = new ContentType("text/html");
	public static final ContentType TEXT_PLAIN = new ContentType("text/plain");
	public static final ContentType TEXT_XML = new ContentType("text/xml");
	public static final ContentType APPLICATION_XML = new ContentType("application/xml");
	public static final ContentType APPLICATION_JSON = new ContentType("application/json");
	public static final ContentType APPLICATION_FORM_URLENCODED = new ContentType("application/x-www-form-urlencoded");
	public static final ContentType APPLICATION_OCTET_STREAM = new ContentType("application/octet-stream");

	/** Infers the content type from a file name, falling back to octet-stream. */
	public static ContentType guessFrom(String path) {
		String guessed = URLConnection.guessContentTypeFromName(path);
		return guessed == null ? APPLICATION_OCTET_STREAM : new ContentType(guessed);
	}

	/**
	 * True when a raw {@code Content-Type} header names this media type. The
	 * comparison ignores case and any parameters, so {@code application/json}
	 * matches {@code application/json; charset=UTF-8}.
	 */
	public boolean matches(String rawHeaderValue) {
		if (rawHeaderValue == null) return false;

		int parameters = rawHeaderValue.indexOf(';');
		String mediaType = parameters < 0 ? rawHeaderValue : rawHeaderValue.substring(0, parameters);
		return mediaType.trim().equalsIgnoreCase(type);
	}

}
