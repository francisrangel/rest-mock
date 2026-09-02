package org.simulatest.restmock.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class LogSafeTest {

	private Map<String, List<String>> headers(String... namesAndValues) {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		for (int i = 0; i < namesAndValues.length; i += 2) {
			headers.put(namesAndValues[i], List.of(namesAndValues[i + 1]));
		}
		return headers;
	}

	@Test
	public void maskHeadersHidesSensitiveValues() {
		Map<String, List<String>> masked = LogSafe.maskHeaders(headers(
			"Authorization", "Bearer secret",
			"Proxy-Authorization", "Basic secret",
			"Cookie", "session=abc",
			"Set-Cookie", "session=abc",
			"X-Api-Key", "key-123"));

		assertEquals(List.of("***"), masked.get("Authorization"));
		assertEquals(List.of("***"), masked.get("Proxy-Authorization"));
		assertEquals(List.of("***"), masked.get("Cookie"));
		assertEquals(List.of("***"), masked.get("Set-Cookie"));
		assertEquals(List.of("***"), masked.get("X-Api-Key"));
	}

	@Test
	public void maskHeadersMatchesHeaderNamesIgnoringCase() {
		Map<String, List<String>> masked = LogSafe.maskHeaders(headers("authorization", "Bearer secret"));

		assertEquals(List.of("***"), masked.get("authorization"));
	}

	@Test
	public void maskHeadersLeavesOrdinaryHeadersIntact() {
		Map<String, List<String>> masked = LogSafe.maskHeaders(headers(
			"Content-Type", "application/json",
			"Authorization", "Bearer secret"));

		assertEquals(List.of("application/json"), masked.get("Content-Type"));
		assertEquals(List.of("***"), masked.get("Authorization"));
	}

	@Test
	public void maskHeadersHandlesNoHeaders() {
		assertEquals(Map.of(), LogSafe.maskHeaders(null));
		assertEquals(Map.of(), LogSafe.maskHeaders(Map.of()));
	}

	@Test
	public void truncateReturnsShortBodiesUnchanged() {
		String body = "x".repeat(LogSafe.BODY_TRUNCATE_LIMIT);

		assertEquals(body, LogSafe.truncate(body));
	}

	@Test
	public void truncateCutsBodiesOverTheLimitAndReportsTheRemainder() {
		String truncated = LogSafe.truncate("x".repeat(LogSafe.BODY_TRUNCATE_LIMIT + 1));

		assertEquals("x".repeat(LogSafe.BODY_TRUNCATE_LIMIT) + "...<+1 chars>", truncated);
	}

	@Test
	public void truncateHandlesANullBody() {
		assertEquals("", LogSafe.truncate(null));
	}

}
