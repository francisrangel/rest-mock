package org.simulatest.restmock.internal.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A {@code %2F} in a request path is a slash inside a segment, not one between
 * two: {@code /files/reports%2F2024.pdf} asks {@code /files/{name}} for
 * {@code reports/2024.pdf}. Decoding the whole path first loses the
 * difference, so the encoded slash is carried through matching as a mark and
 * only becomes a slash again in the capture.
 */
final class EncodedSlash {

	/**
	 * A character no stub path contains, so a marked segment can only match a
	 * placeholder. A request that really sends %00 is read as sending %2F.
	 */
	private static final char MARK = '\0';
	private static final String ENCODED_MARK = "%00";

	private static final Pattern ENCODED_SLASH = Pattern.compile("%2F", Pattern.CASE_INSENSITIVE);

	private EncodedSlash() { }

	/** The decoded path, with every encoded slash marked instead of decoded. */
	static String mark(String rawPath) {
		// The decoder reads '+' as a space, which is true of a query and not of a path.
		return QueryString.decode(ENCODED_SLASH.matcher(rawPath.replace("+", "%2B")).replaceAll(ENCODED_MARK));
	}

	/** The captures as the client meant them, each mark a slash again. */
	static Map<String, String> restore(Map<String, String> captures) {
		Map<String, String> restored = new LinkedHashMap<>(captures);
		restored.replaceAll((name, value) -> value.replace(MARK, '/'));
		return restored;
	}

	/** A marked path as the client sent it, for a message somebody will read. */
	static String show(String marked) {
		return marked.replace(String.valueOf(MARK), "%2F");
	}

}
