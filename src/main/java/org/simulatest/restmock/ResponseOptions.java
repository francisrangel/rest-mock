package org.simulatest.restmock;

import java.time.Duration;
import java.util.Objects;

import org.simulatest.restmock.internal.response.Response;

/**
 * Fluent options applied after the response body has been chosen.
 *
 * Returned by every {@code thenReturn*} method on {@code RestMockResponse}.
 * All methods return {@code this} so calls can be chained. Instances come from
 * the library; there is no public constructor.
 */
public class ResponseOptions {

	private final Response response;

	ResponseOptions(Response response) {
		this.response = response;
	}

	/** Sets a response header. Replaces any previous value for the same header name. */
	public ResponseOptions withHeader(String name, String value) {
		response.addHeader(name, value);
		return this;
	}

	/** Overrides the HTTP status code. Defaults to 200, or to the code passed to {@code thenReturnErrorCodeWithMessage}. Last call wins. */
	public ResponseOptions withStatus(int status) {
		response.setResponseStatus(status);
		return this;
	}

	/**
	 * Sleeps for {@code millis} before sending the response. Useful for simulating
	 * slow upstreams. The delay applies to this route only: requests to other
	 * routes are served concurrently and are not held up. Last call wins.
	 */
	public ResponseOptions withDelay(long millis) {
		if (millis < 0) throw new IllegalArgumentException("Delay must not be negative, but was " + millis + " ms.");

		response.setDelayMillis(millis);
		return this;
	}

	/**
	 * The same delay, said out loud: {@code withDelay(Duration.ofSeconds(2))}
	 * cannot be misread the way {@code withDelay(2000)} can.
	 */
	public ResponseOptions withDelay(Duration delay) {
		return withDelay(Objects.requireNonNull(delay, "delay").toMillis());
	}

}
