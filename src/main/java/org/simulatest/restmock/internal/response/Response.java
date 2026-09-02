package org.simulatest.restmock.internal.response;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.simulatest.restmock.ReceivedRequest;

/**
 * What a stubbed route sends back: a status, headers, a content type, a delay,
 * and the bytes of the body.
 *
 * {@link Template} renders a text body with {@code ${...}} substitution;
 * {@link Binary} hands its bytes over untouched.
 */
public abstract class Response {

	private final Map<String, String> header = new ConcurrentHashMap<>();

	// Written by the test thread via ResponseOptions, read by the server's
	// worker threads. Volatile (and a concurrent map) so a status, delay, or
	// header set after the route was registered is visible to whoever serves it.
	private volatile int responseStatus = HttpURLConnection.HTTP_OK;
	private volatile long delayMillis;

	public abstract ContentType getContentType();

	/**
	 * True when the body is text: it is safe to preview in logs, and the
	 * response declares an explicit UTF-8 charset.
	 */
	public abstract boolean isTextual();

	/** The bytes to write for this response, given what the request carried. */
	public abstract byte[] render(Map<String, String> parameters);

	/** The response to serve for {@code request}: this one, unless a subclass decides per request. */
	public Response resolve(ReceivedRequest request) {
		return this;
	}

	/**
	 * False when {@link #render} never reads its parameters, so the caller can
	 * skip collecting them: parsing a request body is the expensive part of
	 * serving a static stub.
	 */
	public boolean usesParameters() {
		return true;
	}

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	public long getDelayMillis() {
		return delayMillis;
	}

	public void setDelayMillis(long delayMillis) {
		this.delayMillis = delayMillis;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(header);
	}

	public void addHeader(String name, String value) {
		header.put(name, value);
	}

}
