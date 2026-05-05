package restmock;

import restmock.internal.response.Response;

/**
 * Fluent options applied after the response body has been chosen.
 *
 * Returned by every {@code thenReturn*} method on {@code RestMockResponse}.
 * All methods return {@code this} so calls can be chained.
 */
public class ResponseOptions {

	private final Response response;

	public ResponseOptions(Response response) {
		this.response = response;
	}

	/** Sets a response header. Replaces any previous value for the same header name. */
	public ResponseOptions withHeader(String property, String value) {
		response.addHeader(property, value);
		return this;
	}

	/** Overrides the HTTP status code. Defaults to 200, or to the code passed to {@code thenReturnErrorCodeWithMessage}. Last call wins. */
	public ResponseOptions withStatus(int status) {
		response.setResponseStatus(status);
		return this;
	}

	/**
	 * Sleeps for {@code millis} before sending the response. Useful for simulating
	 * slow upstreams. Blocks the server thread; concurrent requests under delay
	 * are serialized. Last call wins.
	 */
	public ResponseOptions withDelay(long millis) {
		response.setDelayMillis(millis);
		return this;
	}

}
