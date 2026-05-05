package restmock;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import restmock.http.HttpMethod;

/**
 * A single request the mock server received.
 *
 * {@code path} is the path component only (no query string); {@code query} is
 * the raw query string or {@code null} if absent. {@code body} is the request
 * body decoded as UTF-8; for empty bodies it is the empty string. {@code headers}
 * is unmodifiable; keys are canonicalized to first-letter-uppercase form
 * ({@code Content-Type}, {@code User-Agent}) by the JDK HTTP server, so look
 * them up using that exact case.
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
		headers = Collections.unmodifiableMap(headers);
	}

}
