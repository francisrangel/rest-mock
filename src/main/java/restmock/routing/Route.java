package restmock.routing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import restmock.http.HttpMethod;

public class Route {

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/{}]+)\\}");

	private final HttpMethod method;
	private final String uri;
	private final Pattern pattern;
	private final List<String> captureNames;

	public Route(HttpMethod method, String uri) {
		this.method = Objects.requireNonNull(method, "method");
		this.uri = Objects.requireNonNull(uri, "uri");
		this.captureNames = new ArrayList<>();
		this.pattern = compile(uri, captureNames);
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
		return other.uri.equals(uri) && other.method.equals(method);
	}

	private static Pattern compile(String uri, List<String> captureNames) {
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
		return Pattern.compile(regex.toString());
	}

}
