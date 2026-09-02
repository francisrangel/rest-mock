package org.simulatest.restmock.internal.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.internal.response.NotConfigured;
import org.simulatest.restmock.internal.response.Response;

/**
 * The stubbed routes, and what a request path is answered with.
 *
 * A route holds one or more responses. They are served in the order they were
 * added, and the last one repeats, so a test can say "the upstream failed
 * once" and then move on.
 *
 * HEAD is answered beyond what was registered: {@link #lookup} serves it from
 * the GET route, and the server sends the headers and withholds the body. An
 * explicit HEAD stub always wins. OPTIONS is the controller's to answer, from
 * what {@link #methodsFor} says the path serves.
 */
public class RouteManager {

	private static final Logger log = LoggerFactory.getLogger(RouteManager.class);

	/**
	 * Replaced wholesale on every registration rather than mutated in place: the
	 * server threads iterate this map while a test thread registers routes, and a
	 * LinkedHashMap cannot be read and written at once. Registration happens a
	 * handful of times per test, so copying is free; keeping LinkedHashMap keeps
	 * the insertion order that {@link #lookup} relies on to break ties.
	 */
	private volatile Map<Route, Stub> routes = new LinkedHashMap<>();

	/** Registers {@code response} as the only response for {@code route}, replacing any sequence it had. */
	public synchronized void registerRoute(Route route, Response response) {
		Stub previous = put(route, new Stub(List.of(response)));

		if (previous != null && !(previous.last() instanceof NotConfigured) && !(response instanceof NotConfigured)) {
			log.warn("Replacing existing route {} {} (previous response: {}, new response: {})",
				route.getMethod(), route.getUri(),
				previous.last().getClass().getSimpleName(), response.getClass().getSimpleName());
		} else {
			log.debug("Registered route {} {} -> {}",
				route.getMethod(), route.getUri(), response.getClass().getSimpleName());
		}
	}

	/** Queues {@code response} after the ones {@code route} already has. */
	public synchronized void appendRoute(Route route, Response response) {
		List<Response> responses = new ArrayList<>(routes.get(route).responses());
		responses.add(response);
		put(route, new Stub(List.copyOf(responses)));

		log.debug("Queued response {} for route {} {}", responses.size(), route.getMethod(), route.getUri());
	}

	/** The copy-on-write swap; see the field comment on {@link #routes}. Returns what the route held before. */
	private Stub put(Route route, Stub stub) {
		Map<Route, Stub> updated = new LinkedHashMap<>(routes);
		Stub previous = updated.put(route, stub);
		routes = updated;
		return previous;
	}

	/** The response most recently registered for {@code route}, or null. */
	public Response get(Route route) {
		Stub stub = routes.get(route);
		return stub == null ? null : stub.last();
	}

	/** The route answering {@code method} at {@code path}: an explicit stub, or for HEAD the GET route. */
	public Optional<Match> lookup(HttpMethod method, String path) {
		Optional<Match> match = find(method, path);

		if (match.isEmpty() && method == HttpMethod.HEAD) return find(HttpMethod.GET, path);
		return match;
	}

	/**
	 * The most specific match wins, measured by how few placeholders a route
	 * captures, so {@code /users/me} beats {@code /users/{id}}. Between routes
	 * that capture the same number, the last registered wins. Only the winner
	 * advances its sequence.
	 */
	private Optional<Match> find(HttpMethod method, String path) {
		Entry<Route, Stub> best = null;
		Map<String, String> bestCaptures = null;

		for (Entry<Route, Stub> entry : routes.entrySet()) {
			Route route = entry.getKey();
			Optional<Map<String, String>> captures = route.match(method, path);
			if (captures.isEmpty()) continue;

			if (best == null || route.captureCount() <= best.getKey().captureCount()) {
				best = entry;
				bestCaptures = captures.get();
			}
		}

		return best == null
			? Optional.empty()
			: Optional.of(new Match(best.getKey(), best.getValue().next(), bestCaptures));
	}

	/**
	 * Every method {@code path} answers: what was registered, plus HEAD
	 * wherever GET is and OPTIONS wherever anything is. Empty when nothing is
	 * stubbed for the path.
	 */
	public Set<HttpMethod> methodsFor(String path) {
		Set<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);

		for (Route route : routes.keySet()) {
			if (route.matchesPath(path)) methods.add(route.getMethod());
		}

		if (methods.isEmpty()) return methods;

		if (methods.contains(HttpMethod.GET)) methods.add(HttpMethod.HEAD);
		methods.add(HttpMethod.OPTIONS);
		return methods;
	}

	public synchronized void clean() {
		routes = new LinkedHashMap<>();
	}

	/** Every registered route, in registration order. Used to explain a 404. */
	public List<Route> registeredRoutes() {
		return List.copyOf(routes.keySet());
	}

	public record Match(Route route, Response response, Map<String, String> pathCaptures) {
		public Match {
			pathCaptures = Collections.unmodifiableMap(pathCaptures);
		}
	}

	/** The responses queued for one route and how many have been served. */
	private record Stub(List<Response> responses, AtomicInteger served) {

		Stub(List<Response> responses) {
			this(responses, new AtomicInteger());
		}

		Response next() {
			return responses.get(Math.min(served.getAndIncrement(), responses.size() - 1));
		}

		Response last() {
			return responses.get(responses.size() - 1);
		}

	}

}
