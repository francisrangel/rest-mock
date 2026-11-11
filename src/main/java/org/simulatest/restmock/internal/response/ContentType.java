package org.simulatest.restmock.internal.response;

import java.net.URLConnection;
import java.util.Locale;

public record ContentType(String type) {

	public static final ContentType TEXT_HTML = new ContentType("text/html");
	public static final ContentType TEXT_PLAIN = new ContentType("text/plain");
	public static final ContentType TEXT_XML = new ContentType("text/xml");
	public static final ContentType APPLICATION_XML = new ContentType("application/xml");
	public static final ContentType APPLICATION_JSON = new ContentType("application/json");
	public static final ContentType APPLICATION_FORM_URLENCODED = new ContentType("application/x-www-form-urlencoded");
	public static final ContentType APPLICATION_OCTET_STREAM = new ContentType("application/octet-stream");

	/**
	 * The header value for a UTF-8 text body. Without the charset a client
	 * applying the historical text/* default decodes "cafe" with an accent as
	 * mojibake.
	 */
	public String utf8() {
		return type + "; charset=utf-8";
	}

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
		return rawHeaderValue != null && mediaType(rawHeaderValue).equalsIgnoreCase(type);
	}

	/**
	 * True when a raw {@code Content-Type} header names a media type with this
	 * structured suffix, as {@code application/vnd.api+json} carries {@code +json}:
	 * the suffix says how the body is written, whatever the type before it.
	 */
	public static boolean hasSuffix(String rawHeaderValue, String suffix) {
		return rawHeaderValue != null && mediaType(rawHeaderValue).toLowerCase(Locale.ROOT).endsWith(suffix);
	}

	private static String mediaType(String rawHeaderValue) {
		return rawHeaderValue.split(";", 2)[0].trim();
	}

}
