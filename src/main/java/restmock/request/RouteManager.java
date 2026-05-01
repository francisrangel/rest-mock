package restmock.request;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import restmock.response.Response;

public class RouteManager {

	private static RouteManager instance;
	private Map<Route, Response> routes = new LinkedHashMap<>();

	private RouteManager() { }

	public static RouteManager getInstance() {
		if (instance == null) instance = new RouteManager();
		return instance;
	}

	public void registerRoute(Route route, Response response) {
		routes.put(route, response);
	}

	public Response get(Route route) {
		return routes.get(route);
	}

	public Optional<Match> lookup(HttpMethod method, String path) {
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

	public Set<HttpMethod> methodsFor(String path) {
		Set<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
		for (Route route : routes.keySet()) {
			if (route.match(route.getMethod(), path).isPresent()) methods.add(route.getMethod());
		}
		return methods;
	}

	public void clean() {
		routes = new LinkedHashMap<>();
	}

	public static final class Match {
		public final Route route;
		public final Response response;
		public final Map<String, String> pathCaptures;

		public Match(Route route, Response response, Map<String, String> pathCaptures) {
			this.route = route;
			this.response = response;
			this.pathCaptures = pathCaptures;
		}
	}

}
