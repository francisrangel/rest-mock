package restmock.http;

/** HTTP methods the mock server can stub. */
public enum HttpMethod {

	GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

	private static final HttpMethod[] VALUES = values();

	/** Case-insensitive lookup. Throws {@link IllegalArgumentException} if {@code method} isn't one of the known names. */
	public static HttpMethod byString(String method) {
		for (HttpMethod value : VALUES)
			if (value.name().equalsIgnoreCase(method))
				return value;

		throw new IllegalArgumentException(method + " isn't a HttpMethod");
	}

}
