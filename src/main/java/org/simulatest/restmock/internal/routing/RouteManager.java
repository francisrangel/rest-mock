package org.simulatest.restmock.internal.routing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.internal.response.NotConfigured;
import org.simulatest.restmock.internal.response.Response;

/**
 * The stubbed routes, and what a request path is answered with.
 *
 * Two methods are answered beyond what was registered: HEAD is served by the
 * GET route (the server sends its headers and withholds the body), and
 * OPTIONS is served for any path that has a route at all. An explicit stub for
 * either always wins.
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
	private volatile Map<Route, Response> routes = new LinkedHashMap<>();

	public synchronized void registerRoute(Route route, Response response) {
		Map<Route, Response> updated = new LinkedHashMap<>(routes);
		Response previous = updated.put(route, response);
		routes = updated;

		if (previous != null && !(previous instanceof NotConfigured) && !(response instanceof NotConfigured)) {
			log.warn("Replacing existing route {} {} (previous response: {}, new response: {})",
				route.getMethod(), route.getUri(),
				previous.getClass().getSimpleName(), response.getClass().getSimpleName());
		} else {
			log.debug("Registered route {} {} -> {}",
				route.getMethod(), route.getUri(), response.getClass().getSimpleName());
		}
	}

	public Response get(Route route) {
		return routes.get(route);
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
	 * that capture the same number, the last registered wins.
	 */
	private Optional<Match> find(HttpMethod method, String path) {
		Match best = null;

		for (Entry<Route, Response> entry : routes.entrySet()) {
			Route route = entry.getKey();
			Optional<Map<String, String>> captures = route.match(method, path);
			if (captures.isEmpty()) continue;

			if (best == null || route.captureCount() <= best.route.captureCount()) {
				best = new Match(route, entry.getValue(), captures.get());
			}
		}

		return Optional.ofNullable(best);
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

}
