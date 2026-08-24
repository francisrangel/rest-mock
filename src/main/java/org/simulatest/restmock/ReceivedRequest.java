package org.simulatest.restmock;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import org.simulatest.restmock.internal.http.QueryString;

/**
 * A single request the mock server received.
 *
 * {@code path} is the path component only (no query string); {@code query} is
 * the raw query string or {@code null} if absent. {@code body} is the request
 * body decoded as UTF-8; for empty bodies it is the empty string.
 *
 * Read headers with {@link #header(String)} and query parameters with
 * {@link #queryParam(String)} rather than picking through {@link #headers()} and
 * {@link #query()}: the JDK HTTP server rewrites every header name to
 * first-letter-uppercase, so a header sent as {@code Content-Type} is stored
 * under {@code Content-type} and an exact-match lookup for the name you sent
 * finds nothing. The accessors here are case-insensitive, as HTTP itself is.
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

	/** The first value of {@code name}, matched case-insensitively. Empty if the header was not sent. */
	public Optional<String> header(String name) {
		return headerValues(name).stream().findFirst();
	}

	/** Every value of {@code name}, matched case-insensitively, in the order the map holds them. */
	public List<String> headerValues(String name) {
		return headers.entrySet().stream()
			.filter(header -> header.getKey().equalsIgnoreCase(name))
			.flatMap(header -> header.getValue().stream())
			.toList();
	}

	/**
	 * The first value of the query parameter {@code name}, URL-decoded. Matched
	 * exactly: unlike header names, query parameter names are case-sensitive.
	 */
	public Optional<String> queryParam(String name) {
		return QueryString.first(query, name);
	}

	/** Every value sent for the query parameter {@code name}, URL-decoded, in order. */
	public List<String> queryParamValues(String name) {
		return QueryString.all(query, name);
	}

}
