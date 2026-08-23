package org.simulatest.restmock.internal.response;

/**
 * Escapes substituted values so a request cannot break the response document.
 *
 * Only characters that would corrupt the format are touched, so the common
 * cases - an id, a name, a number - pass through unchanged.
 */
final class Escape {

	private Escape() { }

	/** Escapes for a JSON string literal. */
	static String json(String value) {
		StringBuilder out = new StringBuilder(value.length());

		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				case '\b' -> out.append("\\b");
				case '\f' -> out.append("\\f");
				default -> {
					if (character < 0x20) out.append(String.format("\\u%04x", (int) character));
					else out.append(character);
				}
			}
		}

		return out.toString();
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
