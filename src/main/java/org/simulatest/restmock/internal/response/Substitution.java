package org.simulatest.restmock.internal.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simulatest.restmock.internal.Placeholders;

/**
 * Fills the {@code ${...}} placeholders of a text from what a request carried.
 * Bodies and header values share it.
 */
final class Substitution {

	/** A placeholder, or the doubled dollar that makes one literal. */
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\$\\{|\\$\\{(.+?)\\}");
	private static final String LITERAL_PLACEHOLDER = Matcher.quoteReplacement("${");

	/** Enough names to spot the typo, few enough to stay readable in a failure. */
	private static final int NAMES_IN_ERROR = 20;

	private Substitution() { }

	/** Fills {@code text} with the values as they came, for a text with no format a value could break. */
	static String fill(String text, Map<String, String> parameters) {
		return fill(text, parameters, UnaryOperator.identity());
	}

	/**
	 * A name with no value is a mistake in the stub, so it fails here rather
	 * than shipping {@code ${nmae}} to the client and letting a test that only
	 * checks the status pass.
	 */
	static String fill(String text, Map<String, String> parameters, UnaryOperator<String> escape) {
		return PLACEHOLDER.matcher(text).replaceAll(match -> {
			String name = match.group(1);
			if (name == null) return LITERAL_PLACEHOLDER;

			String value = parameters.get(Placeholders.key(name));
			if (value == null) throw unresolved(name, parameters);
			return Matcher.quoteReplacement(escape.apply(value));
		});
	}

	private static UnresolvedPlaceholderException unresolved(String name, Map<String, String> parameters) {
		return new UnresolvedPlaceholderException(
			"No value for ${" + name + "}. Available names: " + available(parameters));
	}

	/**
	 * Lists the names the stub author wrote, and only counts the headers. An
	 * ordinary request carries five to ten of them, and spelling them all out
	 * buried the two or three names anybody is actually looking for.
	 */
	private static String available(Map<String, String> parameters) {
		List<String> authored = new ArrayList<>();
		int headers = 0;

		for (String name : parameters.keySet()) {
			if (Placeholders.isHeader(name)) headers++;
			else authored.add(name);
		}

		return headers == 0 ? listed(authored) : listed(authored) + "; " + headersCounted(headers);
	}

	private static String headersCounted(int headers) {
		return "plus " + headers + (headers == 1 ? " request header" : " request headers")
			+ " as ${" + Placeholders.HEADER_PREFIX + "NAME}";
	}

	private static String listed(List<String> names) {
		if (names.isEmpty()) return "(none)";
		if (names.size() <= NAMES_IN_ERROR) return String.join(", ", names);

		return String.join(", ", names.subList(0, NAMES_IN_ERROR))
			+ " and " + (names.size() - NAMES_IN_ERROR) + " more";
	}

}
