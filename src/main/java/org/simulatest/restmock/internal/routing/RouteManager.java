package org.simulatest.restmock.internal.routing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.internal.response.NotConfigured;
import org.simulatest.restmock.internal.response.Response;

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

	public RouteManager() { }

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

	public Optional<Match> lookup(HttpMethod method, String path) {
		Match best = null;
		int matchCount = 0;
		for (Entry<Route, Response> entry : routes.entrySet()) {
			Route route = entry.getKey();
			Optional<Map<String, String>> captures = route.match(method, path);
			if (captures.isEmpty()) continue;

			matchCount++;
			if (best == null || route.captureCount() <= best.route.captureCount()) {
				best = new Match(route, entry.getValue(), captures.get());
			}
		}
		if (matchCount > 1 && log.isDebugEnabled()) {
			log.debug("{} routes matched {} {}, picked {} (captures={})",
				matchCount, method, path, best.route.getUri(), best.pathCaptures);
		}
		return Optional.ofNullable(best);
	}

	public Set<HttpMethod> methodsFor(String path) {
		Set<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
		for (Route route : routes.keySet()) {
			if (route.matchesPath(path)) methods.add(route.getMethod());
		}
		return methods;
	}

	public synchronized void clean() {
		routes = new LinkedHashMap<>();
	}

	public int size() {
		return routes.size();
	}

	public record Match(Route route, Response response, Map<String, String> pathCaptures) {
		public Match {
			pathCaptures = Collections.unmodifiableMap(pathCaptures);
		}
	}

}
