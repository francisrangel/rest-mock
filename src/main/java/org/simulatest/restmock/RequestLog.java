package org.simulatest.restmock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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
 * Safe to read from any thread; every accessor takes a snapshot, so iteration is
 * never disturbed by requests arriving as you read.
 *
 * Cleared by {@link RestMock#clean()} and by {@link RestMockExtension} between tests.
 */
public class RequestLog {

	/**
	 * Guarded by this. A copy-on-write list looks tempting for a
	 * write-once-read-rarely log, but it is exactly backwards here: every
	 * recorded request copied the whole backing array, so a test firing a few
	 * thousand requests paid O(n^2) to build a list nobody read until the end.
	 */
	private final List<ReceivedRequest> requests = new ArrayList<>();

	/** Every recorded request, in arrival order. */
	public synchronized List<ReceivedRequest> all() {
		return List.copyOf(requests);
	}

	/** Recorded requests whose path equals {@code path} exactly. */
	public List<ReceivedRequest> forPath(String path) {
		return all().stream()
			.filter(r -> r.path().equals(path))
			.toList();
	}

	/** Recorded requests with the given HTTP method. */
	public List<ReceivedRequest> forMethod(HttpMethod method) {
		return all().stream()
			.filter(r -> r.method() == method)
			.toList();
	}

	/** Recorded requests matching both method and exact path. */
	public List<ReceivedRequest> forRoute(HttpMethod method, String path) {
		return all().stream()
			.filter(r -> r.method() == method && r.path().equals(path))
			.toList();
	}

	/** Total recorded requests. */
	public synchronized int count() {
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
	public synchronized boolean isEmpty() {
		return requests.isEmpty();
	}

	/** The most recent request, or empty if none. */
	public Optional<ReceivedRequest> last() {
		return last(all());
	}

	/** The most recent request that hit this exact path, or empty if none. */
	public Optional<ReceivedRequest> lastForPath(String path) {
		return last(forPath(path));
	}

	/** Invoked by the server when a request arrives. Not public: a forged entry would make every assertion here worthless. */
	synchronized void add(ReceivedRequest request) {
		requests.add(request);
	}

	/** Invoked by {@link RestMock#clean()} and {@link RestMock#stopServer()}. */
	synchronized void clear() {
		requests.clear();
	}

	private static Optional<ReceivedRequest> last(List<ReceivedRequest> candidates) {
		return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(candidates.size() - 1));
	}

}
