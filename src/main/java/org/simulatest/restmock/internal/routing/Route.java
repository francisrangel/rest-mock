package org.simulatest.restmock.internal.routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simulatest.restmock.HttpMethod;

public class Route {

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/{}]+)\\}");

	private final HttpMethod method;
	private final String uri;
	private final Pattern pattern;
	private final List<String> captureNames;

	public Route(HttpMethod method, String uri) {
		this.method = Objects.requireNonNull(method, "method");
		this.uri = Objects.requireNonNull(uri, "uri");

		validate(uri);

		Compiled compiled = compile(uri);
		this.pattern = compiled.pattern();
		this.captureNames = compiled.captureNames();
		validateCaptureNames(uri, this.captureNames);
	}

	public Route(String method, String uri) {
		this(HttpMethod.byString(method), uri);
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	public int captureCount() {
		return captureNames.size();
	}

	/** True when this route's path pattern matches {@code requestPath}, ignoring the method. */
	public boolean matchesPath(String requestPath) {
		return pattern.matcher(requestPath).matches();
	}

	public Optional<Map<String, String>> match(HttpMethod requestMethod, String requestPath) {
		if (this.method != requestMethod) return Optional.empty();

		Matcher matcher = pattern.matcher(requestPath);
		if (!matcher.matches()) return Optional.empty();

		Map<String, String> captures = new LinkedHashMap<>();
		for (int i = 0; i < captureNames.size(); i++) {
			captures.put(captureNames.get(i), matcher.group(i + 1));
		}
		return Optional.of(captures);
	}

	@Override
	public int hashCode() {
		return Objects.hash(method, uri);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Route other)) return false;
		return other.uri.equals(uri) && other.method == method;
	}

	/**
	 * Rejects a stub URI that can never match, at the {@code when*} call that
	 * wrote it. Every one of these used to compile into a pattern the request
	 * path could not possibly equal, so the mistake surfaced later as a bare 404
	 * on a route the test could see right there in the source.
	 */
	private static void validate(String uri) {
		if (!uri.startsWith("/"))
			throw invalid(uri, "must start with '/'");

		int query = uri.indexOf('?');
		if (query >= 0)
			throw invalid(uri, "must not contain a query string; stub the path \"" + uri.substring(0, query)
				+ "\" instead and read the query with ${name} or from RestMock.requests()");

		if (uri.indexOf('#') >= 0)
			throw invalid(uri, "must not contain a fragment; fragments never reach the server");

		validatePlaceholders(uri);
	}

	/**
	 * A malformed placeholder is worse than a syntax error: a path template
	 * missing its closing brace compiles to a literal path with a brace in it,
	 * and quietly matches nothing.
	 */
	private static void validatePlaceholders(String uri) {
		int position = 0;
		while (position < uri.length()) {
			char character = uri.charAt(position);

			if (character == '}')
				throw invalid(uri, "has a '}' with no matching '{'");

			if (character != '{') {
				position++;
				continue;
			}

			int close = uri.indexOf('}', position + 1);
			if (close < 0)
				throw invalid(uri, "has an unclosed '{'; path placeholders look like /users/{id}");

			String name = uri.substring(position + 1, close);
			if (name.isEmpty())
				throw invalid(uri, "has an empty placeholder '{}'; name it, as in /users/{id}");
			if (name.indexOf('/') >= 0 || name.indexOf('{') >= 0)
				throw invalid(uri, "has the malformed placeholder '{" + name + "}'");

			position = close + 1;
		}
	}

	/** Two captures under one name would silently keep whichever matched last. */
	private static void validateCaptureNames(String uri, List<String> names) {
		Set<String> seen = new HashSet<>();
		for (String name : names)
			if (!seen.add(name))
				throw invalid(uri, "uses the placeholder '{" + name + "}' more than once; give each capture its own name");
	}

	private static IllegalArgumentException invalid(String uri, String problem) {
		return new IllegalArgumentException("Stub URI \"" + uri + "\" " + problem + ".");
	}

	private static Compiled compile(String uri) {
		List<String> captureNames = new ArrayList<>();
		Matcher m = PLACEHOLDER.matcher(uri);
		StringBuilder regex = new StringBuilder("^");
		int last = 0;
		while (m.find()) {
			regex.append(Pattern.quote(uri.substring(last, m.start())));
			regex.append("([^/]+)");
			captureNames.add(m.group(1));
			last = m.end();
		}
		regex.append(Pattern.quote(uri.substring(last)));
		regex.append("$");
		return new Compiled(Pattern.compile(regex.toString()), List.copyOf(captureNames));
	}

	private record Compiled(Pattern pattern, List<String> captureNames) { }

}
