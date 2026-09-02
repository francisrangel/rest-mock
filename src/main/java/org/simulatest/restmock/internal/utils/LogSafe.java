package org.simulatest.restmock.internal.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Keeps request and response logging readable and free of credentials. */
public final class LogSafe {

	public static final int BODY_TRUNCATE_LIMIT = 512;

	private static final Set<String> SENSITIVE_HEADERS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	static {
		SENSITIVE_HEADERS.addAll(List.of("Authorization", "Proxy-Authorization", "Cookie", "Set-Cookie", "X-Api-Key"));
	}

	private static final String MASK = "***";

	private LogSafe() {}

	public static String truncate(String body) {
		if (body.length() <= BODY_TRUNCATE_LIMIT) return body;
		return body.substring(0, BODY_TRUNCATE_LIMIT) + "...<+" + (body.length() - BODY_TRUNCATE_LIMIT) + " chars>";
	}

	public static Map<String, List<String>> maskHeaders(Map<String, List<String>> headers) {
		Map<String, List<String>> masked = new LinkedHashMap<>(headers.size());
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String name = entry.getKey();
			masked.put(name, SENSITIVE_HEADERS.contains(name) ? List.of(MASK) : entry.getValue());
		}
		return masked;
	}

}
