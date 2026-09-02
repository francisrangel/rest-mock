package org.simulatest.restmock.internal.response;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

/**
 * Escapes substituted values so a request cannot break the response document.
 *
 * Only characters that would corrupt the format are touched, so the common
 * cases - an id, a name, a number - pass through unchanged.
 */
final class Escape {

	private Escape() { }

	/** Escapes for a JSON string literal, with Jackson's own encoder so the rules cannot drift from the serializer's. */
	static String json(String value) {
		return new String(JsonStringEncoder.getInstance().quoteAsString(value));
	}

	/** Escapes for XML and HTML, in element text and attribute values alike. */
	static String markup(String value) {
		StringBuilder out = new StringBuilder(value.length());

		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append("&quot;");
				case '\'' -> out.append("&#39;");
				default -> out.append(character);
			}
		}

		return out.toString();
	}

}
