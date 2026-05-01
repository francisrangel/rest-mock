package restmock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import restmock.http.HttpMethod;

public class RequestLog {

	private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();

	public List<ReceivedRequest> all() {
		return Collections.unmodifiableList(requests);
	}

	public List<ReceivedRequest> forPath(String path) {
		return requests.stream()
			.filter(r -> r.path().equals(path))
			.toList();
	}

	public List<ReceivedRequest> forMethod(HttpMethod method) {
		return requests.stream()
			.filter(r -> r.method() == method)
			.toList();
	}

	public List<ReceivedRequest> forRoute(HttpMethod method, String path) {
		return requests.stream()
			.filter(r -> r.method() == method && r.path().equals(path))
			.toList();
	}

	public int count() {
		return requests.size();
	}

	public int countForPath(String path) {
		return (int) requests.stream().filter(r -> r.path().equals(path)).count();
	}

	public int countForRoute(HttpMethod method, String path) {
		return (int) requests.stream().filter(r -> r.method() == method && r.path().equals(path)).count();
	}

	public boolean isEmpty() {
		return requests.isEmpty();
	}

	public Optional<ReceivedRequest> last() {
		return requests.isEmpty() ? Optional.empty() : Optional.of(requests.get(requests.size() - 1));
	}

	public Optional<ReceivedRequest> lastForPath(String path) {
		ReceivedRequest result = null;
		for (ReceivedRequest r : requests) {
			if (r.path().equals(path)) result = r;
		}
		return Optional.ofNullable(result);
	}

	public void add(ReceivedRequest request) {
		requests.add(request);
	}

	public void clear() {
		requests.clear();
	}

}
