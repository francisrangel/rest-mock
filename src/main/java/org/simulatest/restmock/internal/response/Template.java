package org.simulatest.restmock.internal.response;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.simulatest.restmock.internal.Placeholders;

/**
 * A text body with {@code ${...}} placeholders, rendered per request from what
 * that request carried.
 */
public abstract class Template extends Response {

	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	/** Enough names to spot the typo, few enough to stay readable in a failure. */
	private static final int NAMES_IN_ERROR = 20;

	private final String content;
	private final boolean templated;

	Template(String body) {
		this.content = body;
		this.templated = body.contains("${");
	}

	@Override
	public boolean isTextual() {
		return true;
	}

	@Override
	public boolean usesParameters() {
		return templated;
	}

	/**
	 * A name with no value is a mistake in the stub, so it fails here rather
	 * than shipping {@code ${nmae}} to the client and letting a test that only
	 * checks the status pass.
	 */
	@Override
	public byte[] render(Map<String, String> parameters) {
		String rendered = PARAMETER_PATTERN.matcher(content).replaceAll(match -> {
			String value = parameters.get(Placeholders.key(match.group(1)));
			if (value == null) throw unresolved(match.group(1), parameters);
			return Matcher.quoteReplacement(escape(value));
		});
		return rendered.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Escapes a substituted value for this body's format. Plain text needs
	 * nothing; JSON, XML, and HTML override it so a value carrying a quote or an
	 * angle bracket cannot produce a malformed document.
	 */
	String escape(String value) {
		return value;
	}

	private static IllegalStateException unresolved(String name, Map<String, String> parameters) {
		return new IllegalStateException(
			"No value for ${" + name + "}. Available names: " + available(parameters));
	}

	/**
	 * Lists the names the stub author wrote, and only counts the headers. An
	 * ordinary request carries five to ten of them, and spelling them all out
	 * buried the two or three names anybody is actually looking for.
	 */
	private static String available(Map<String, String> parameters) {
		if (parameters.isEmpty()) return "(none)";

		List<String> authored = new ArrayList<>();
		int headers = 0;

		for (String name : parameters.keySet()) {
			if (Placeholders.isHeader(name)) headers++;
			else authored.add(name);
		}

		StringBuilder rendered = new StringBuilder(listed(authored));

		if (headers > 0)
			rendered.append(authored.isEmpty() ? "" : "; ")
				.append("plus ").append(headers)
				.append(headers == 1 ? " request header as " : " request headers as ")
				.append("${").append(Placeholders.HEADER_PREFIX).append("NAME}");

		return rendered.toString();
	}

	private static String listed(List<String> names) {
		if (names.isEmpty()) return "";
		if (names.size() <= NAMES_IN_ERROR) return String.join(", ", names);

		return String.join(", ", names.subList(0, NAMES_IN_ERROR))
			+ " and " + (names.size() - NAMES_IN_ERROR) + " more";
	}

	/** The body as written, placeholders and all. */
	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return content;
	}

	static String serialize(ObjectMapper mapper, Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

}
