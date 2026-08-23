package org.simulatest.restmock;

import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * A single request the mock server received.
 *
 * {@code path} is the path component only (no query string); {@code query} is
 * the raw query string or {@code null} if absent. {@code body} is the request
 * body decoded as UTF-8; for empty bodies it is the empty string. {@code headers}
 * is unmodifiable; the JDK HTTP server lowercases each name and capitalizes only
 * its first letter, so a header sent as {@code Content-Type} is stored as
 * {@code Content-type} and {@code X-Tenant} as {@code X-tenant}. Look them up
 * using that exact case, or use {@code ${name}} in a response body, which
 * resolves header names case-insensitively.
 */
public record ReceivedRequest(
	HttpMethod method,
	String path,
	String query,
	Map<String, List<String>> headers,
	String body,
	Instant timestamp
) {

	public ReceivedRequest {
		headers = Map.copyOf(headers);
	}

}
