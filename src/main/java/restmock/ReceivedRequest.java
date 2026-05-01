package restmock;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import restmock.http.HttpMethod;

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
