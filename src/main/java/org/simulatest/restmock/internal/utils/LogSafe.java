package org.simulatest.restmock.internal.utils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class LogSafe {

	public static final int BODY_TRUNCATE_LIMIT = 512;

	private static final Set<String> SENSITIVE_HEADERS;
	static {
		Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		set.add("Authorization");
		set.add("Proxy-Authorization");
		set.add("Cookie");
		set.add("Set-Cookie");
		set.add("X-Api-Key");
		SENSITIVE_HEADERS = Collections.unmodifiableSet(set);
	}

	private static final String MASK = "***";

	private LogSafe() {}

	public static String truncate(String body) {
		if (body == null) return "";
		if (body.length() <= BODY_TRUNCATE_LIMIT) return body;
		return body.substring(0, BODY_TRUNCATE_LIMIT) + "...<+" + (body.length() - BODY_TRUNCATE_LIMIT) + " chars>";
	}

	// Decodes only enough bytes to fill the truncate window, avoiding a full
	// UTF-8 decode of large response bodies on the TRACE path. UTF-8 codepoints
	// are at most 4 bytes, so LIMIT*4 bytes guarantees at least LIMIT chars.
	public static String previewBytes(byte[] body) {
		if (body == null || body.length == 0) return "";
		int sliceMax = BODY_TRUNCATE_LIMIT * 4;
		if (body.length <= sliceMax) return truncate(new String(body, StandardCharsets.UTF_8));
		String head = new String(body, 0, sliceMax, StandardCharsets.UTF_8);
		if (head.length() > BODY_TRUNCATE_LIMIT) head = head.substring(0, BODY_TRUNCATE_LIMIT);
		return head + "...<+" + (body.length - sliceMax) + " bytes>";
	}

	public static String binaryPlaceholder(int byteCount) {
		return "<" + byteCount + " bytes binary>";
	}

	public static Map<String, List<String>> maskHeaders(Map<String, List<String>> headers) {
		if (headers == null) return Map.of();
		Map<String, List<String>> masked = new LinkedHashMap<>(headers.size());
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String name = entry.getKey();
			masked.put(name, SENSITIVE_HEADERS.contains(name) ? List.of(MASK) : entry.getValue());
		}
		return masked;
	}

}
