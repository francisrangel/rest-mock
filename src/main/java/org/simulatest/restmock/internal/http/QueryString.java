package org.simulatest.restmock.internal.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Reads {@code a=1&b=2} pairs, whether they arrived as a URL's query string or
 * as a form-encoded body. Shared so a query parameter reads the same way through
 * a {@code ${name}} placeholder and through {@code ReceivedRequest.queryParam}.
 *
 * A pair with no {@code =} is skipped: there is no value to hand back, and
 * inventing an empty one would make {@code ?debug} look like {@code ?debug=}.
 * Repeated names expose their first value, matching how repeated headers behave.
 */
public final class QueryString {

	private QueryString() { }

	/** Decoded pairs in the order they appeared. */
	public static Map<String, String> parse(String raw) {
		Map<String, String> parameters = new LinkedHashMap<>();
		forEachPair(raw, parameters::putIfAbsent);
		return parameters;
	}

	/** The first value sent under {@code name}, matched exactly. */
	public static Optional<String> first(String raw, String name) {
		return Optional.ofNullable(parse(raw).get(name));
	}

	/** Every value sent under {@code name}, in order. */
	public static List<String> all(String raw, String name) {
		List<String> values = new ArrayList<>();
		forEachPair(raw, (candidate, value) -> {
			if (candidate.equals(name)) values.add(value);
		});
		return values;
	}

	private static void forEachPair(String raw, BiConsumer<String, String> consumer) {
		if (raw == null || raw.isEmpty()) return;

		for (String pair : raw.split("&")) {
			int separator = pair.indexOf('=');
			if (separator < 0) continue;

			consumer.accept(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
		}
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

}
