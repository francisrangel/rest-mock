package org.simulatest.restmock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Records every request the mock server received, in arrival order.
 *
 * Use it to assert that the system under test made the calls you expected:
 *
 *   assertEquals(1, RestMock.requests().countForRoute(HttpMethod.POST, "/orders"));
 *   ReceivedRequest last = RestMock.requests().last().orElseThrow();
 *   assertTrue(last.body().contains("\"sku\":\"ABC\""));
 *
 * Path matching here is exact-string against {@code request.path()}; templates
 * like {@code /users/{id}} are not expanded for filtering. Filter with the literal
 * path the client called, or use {@link #all()} and stream.
 *
 * Safe to read from any thread; iteration sees a consistent snapshot even while
 * the server is recording new requests.
 *
 * Cleared by {@link RestMock#clean()} and by {@link RestMockExtension} between tests.
 */
public class RequestLog {

	private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();

	/** Every recorded request, in arrival order. */
	public List<ReceivedRequest> all() {
		return Collections.unmodifiableList(requests);
	}

	/** Recorded requests whose path equals {@code path} exactly. */
	public List<ReceivedRequest> forPath(String path) {
		return requests.stream()
			.filter(r -> r.path().equals(path))
			.toList();
	}

	/** Recorded requests with the given HTTP method. */
	public List<ReceivedRequest> forMethod(HttpMethod method) {
		return requests.stream()
			.filter(r -> r.method() == method)
			.toList();
	}

	/** Recorded requests matching both method and exact path. */
	public List<ReceivedRequest> forRoute(HttpMethod method, String path) {
		return requests.stream()
			.filter(r -> r.method() == method && r.path().equals(path))
			.toList();
	}

	/** Total recorded requests. */
	public int count() {
		return requests.size();
	}

	/** How many recorded requests had this exact path. */
	public int countForPath(String path) {
		return forPath(path).size();
	}

	/** How many recorded requests matched both method and exact path. */
	public int countForRoute(HttpMethod method, String path) {
		return forRoute(method, path).size();
	}

	/** True if no requests have been recorded. */
	public boolean isEmpty() {
		return requests.isEmpty();
	}

	/** The most recent request, or empty if none. */
	public Optional<ReceivedRequest> last() {
		return last(requests);
	}

	/** The most recent request that hit this exact path, or empty if none. */
	public Optional<ReceivedRequest> lastForPath(String path) {
		return last(forPath(path));
	}

	/** Internal: invoked by the server when a request arrives. */
	public void add(ReceivedRequest request) {
		requests.add(request);
	}

	/** Internal: invoked by {@link RestMock#clean()} and {@link RestMock#stopServer()}. */
	public void clear() {
		requests.clear();
	}

	private static Optional<ReceivedRequest> last(List<ReceivedRequest> candidates) {
		return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(candidates.size() - 1));
	}

}
