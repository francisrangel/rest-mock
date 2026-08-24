package org.simulatest.restmock.internal.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.simulatest.restmock.HttpMethod;

/**
 * Explains, in the 404 body, why nothing answered a request.
 *
 * "Why isn't my mock matching?" is the question this library gets asked most,
 * and an empty 404 answers none of it. The stub is right there in the test, so
 * the server can say what it has and how close the caller got: a path stubbed
 * under a different verb, a typo one character off, or nothing at all.
 */
public final class NoRouteReport {

	/** Enough routes to find the one you meant, few enough to read in a terminal. */
	private static final int ROUTES_LISTED = 20;

	/**
	 * How far a stub may be from the requested path and still be offered as the
	 * likely target. Scaled by length so "/a" does not match "/b", while a long
	 * path tolerates the transposition that actually happens in tests.
	 */
	private static final int MINIMUM_EDIT_BUDGET = 2;
	private static final int LENGTH_DIVISOR = 4;

	private NoRouteReport() { }

	public static String describe(HttpMethod method, String path, List<Route> routes) {
		StringBuilder report = new StringBuilder("No stub for ").append(method).append(' ').append(path);

		if (routes.isEmpty()) {
			return report
				.append("\n\nNothing is stubbed. Call RestMock.when")
				.append(camelCase(method)).append("(\"").append(path)
				.append("\").thenReturn... before the request.")
				.toString();
		}

		hint(method, path, routes).ifPresent(hint -> report.append("\n\n").append(hint));

		report.append("\n\nStubbed routes:");
		for (String line : list(routes)) report.append("\n  ").append(line);

		return report.toString();
	}

	/**
	 * The strongest signal first. A path that other verbs answer is a wrong-verb
	 * mistake and needs no guessing; only when no route serves this path at all
	 * is it worth looking for one spelled almost like it.
	 */
	private static Optional<String> hint(HttpMethod method, String path, List<Route> routes) {
		Set<HttpMethod> otherMethods = methodsAnswering(path, routes);

		if (!otherMethods.isEmpty()) {
			return Optional.of(path + " is stubbed for " + join(otherMethods) + ", not " + method + ".");
		}

		return nearest(method, path, routes)
			.map(route -> "Closest stub: " + route.getMethod() + " " + route.getUri());
	}

	private static Set<HttpMethod> methodsAnswering(String path, List<Route> routes) {
		Set<HttpMethod> methods = new TreeSet<>();
		for (Route route : routes)
			if (route.matchesPath(path)) methods.add(route.getMethod());

		return methods;
	}

	/**
	 * Offered back as the stub was written, braces and all, so the caller reads
	 * the line they can go and fix rather than a path the server invented.
	 */
	private static Optional<Route> nearest(HttpMethod method, String path, List<Route> routes) {
		int budget = Math.max(MINIMUM_EDIT_BUDGET, path.length() / LENGTH_DIVISOR);

		return routes.stream()
			.filter(route -> distanceTo(route, path) <= budget)
			.min(Comparator
				.comparingInt((Route route) -> distanceTo(route, path))
				.thenComparing(route -> route.getMethod() == method ? 0 : 1));
	}

	/**
	 * Placeholders are filled with the request's own segments before measuring,
	 * so {@code /users/{id}} is judged against {@code /users/42} rather than
	 * against its own braces. Without this a template is always several edits
	 * away from any real path and is never the closest stub, which is exactly
	 * when the hint would help most.
	 */
	private static int distanceTo(Route route, String path) {
		return distance(filled(route.getUri(), path), path);
	}

	private static String filled(String uri, String path) {
		String[] template = uri.split("/", -1);
		String[] actual = path.split("/", -1);
		if (template.length != actual.length) return uri;

		StringBuilder filled = new StringBuilder();
		for (int segment = 0; segment < template.length; segment++) {
			if (segment > 0) filled.append('/');
			filled.append(isPlaceholder(template[segment]) ? actual[segment] : template[segment]);
		}
		return filled.toString();
	}

	private static boolean isPlaceholder(String segment) {
		return segment.length() > 2 && segment.startsWith("{") && segment.endsWith("}");
	}

	private static List<String> list(List<Route> routes) {
		List<String> lines = routes.stream()
			.limit(ROUTES_LISTED)
			.map(route -> String.format("%-7s %s", route.getMethod(), route.getUri()))
			.collect(Collectors.toCollection(ArrayList::new));

		if (routes.size() > ROUTES_LISTED)
			lines.add("and " + (routes.size() - ROUTES_LISTED) + " more");

		return lines;
	}

	private static String join(Set<HttpMethod> methods) {
		return methods.stream().map(Enum::name).collect(Collectors.joining(", "));
	}

	private static String camelCase(HttpMethod method) {
		String name = method.name();
		return name.charAt(0) + name.substring(1).toLowerCase();
	}

	/** Levenshtein distance, single-row so an accidental fleet of routes stays cheap. */
	private static int distance(String left, String right) {
		int[] previous = new int[right.length() + 1];
		for (int column = 0; column <= right.length(); column++) previous[column] = column;

		for (int row = 1; row <= left.length(); row++) {
			int diagonal = previous[0];
			previous[0] = row;

			for (int column = 1; column <= right.length(); column++) {
				int replaced = diagonal + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
				diagonal = previous[column];
				previous[column] = Math.min(Math.min(previous[column] + 1, previous[column - 1] + 1), replaced);
			}
		}

		return previous[right.length()];
	}

}
