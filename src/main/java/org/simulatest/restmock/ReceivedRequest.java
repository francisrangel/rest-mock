package org.simulatest.restmock;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.simulatest.restmock.internal.http.QueryString;

/**
 * A single request the mock server received.
 *
 * {@code path} is the path component only (no query string); {@code query} is
 * the raw query string or {@code null} if absent. {@code body} is the request
 * body decoded as UTF-8 whatever charset the request declared, so a binary
 * upload cannot be read back byte for byte; for empty bodies it is the empty
 * string.
 *
 * Header names are matched case-insensitively, as HTTP itself is: the JDK
 * server rewrites a header sent as {@code Content-Type} to {@code Content-type},
 * and {@link #header(String)}, {@link #headerValues(String)} and the
 * {@link #headers()} map all find it under either spelling. Query parameter
 * names, by contrast, are matched exactly.
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
		headers = caseInsensitiveCopy(headers);
	}

	/** The first value of {@code name}, matched case-insensitively. Empty if the header was not sent. */
	public Optional<String> header(String name) {
		return headerValues(name).stream().findFirst();
	}

	/** Every value of {@code name}, matched case-insensitively, in the order they were sent. */
	public List<String> headerValues(String name) {
		return headers.getOrDefault(name, List.of());
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

	private static Map<String, List<String>> caseInsensitiveCopy(Map<String, List<String>> headers) {
		Map<String, List<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		headers.forEach((name, values) -> copy.put(name, List.copyOf(values)));
		return Collections.unmodifiableMap(copy);
	}

}
