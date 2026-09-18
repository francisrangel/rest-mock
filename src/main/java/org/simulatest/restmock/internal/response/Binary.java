package org.simulatest.restmock.internal.response;

import java.util.Map;

/** Raw bytes served as-is: no template, no substitution, no charset. */
public final class Binary extends Response {

	private final byte[] bytes;

	/** Copies the caller's array, so a test that reuses its buffer after stubbing does not change what the route serves. */
	public Binary(byte[] bytes, ContentType contentType) {
		super(contentType);
		this.bytes = bytes.clone();
	}

	/**
	 * Hands the live array to the write path rather than copying per request:
	 * the caller only writes it out, and a fixture can be large.
	 */
	@Override
	public byte[] render(Map<String, String> parameters) {
		return bytes;
	}

	@Override
	public boolean isTextual() {
		return false;
	}

}
