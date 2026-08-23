package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ReceivedRequestTest {

	private ReceivedRequest requestWith(Map<String, List<String>> headers) {
		return new ReceivedRequest(HttpMethod.GET, "/users", null, headers, "", Instant.now());
	}

	@Test
	public void headersAreCopiedSoLaterChangesToTheSourceAreNotVisible() {
		Map<String, List<String>> source = new HashMap<>();
		source.put("Content-Type", List.of("application/json"));

		ReceivedRequest request = requestWith(source);
		source.put("X-Added-Later", List.of("nope"));

		assertEquals(1, request.headers().size());
		assertEquals("application/json", request.headers().get("Content-Type").get(0));
		assertNull(request.headers().get("X-Added-Later"));
	}

	@Test
	public void headersCannotBeModifiedByCallers() {
		ReceivedRequest request = requestWith(new HashMap<>());

		assertThrows(UnsupportedOperationException.class,
			() -> request.headers().put("X-Trace", List.of("abc")));
	}

	@Test
	public void headersMayBeEmpty() {
		assertTrue(requestWith(new HashMap<>()).headers().isEmpty());
	}

}
