package restmock.request;

public enum HttpMethod {

	GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

	private static final HttpMethod[] VALUES = values();

	public static HttpMethod byString(String method) {
		for (HttpMethod value : VALUES)
			if (value.name().equalsIgnoreCase(method))
				return value;

		throw new IllegalArgumentException(method + " isn't a HttpMethod");
	}

}
