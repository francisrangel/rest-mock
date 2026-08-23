package org.simulatest.restmock.internal.response;

import java.util.Map;

public final class Binary extends Response {

	private final byte[] bytes;
	private final ContentType contentType;

	/**
	 * Copies the caller's array. Holding onto it meant a test that reuses a
	 * buffer after stubbing silently changed what the route serves.
	 */
	public Binary(byte[] bytes, ContentType contentType) {
		super("");
		this.bytes = bytes.clone();
		this.contentType = contentType;
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

	@Override
	public ContentType getContentType() {
		return contentType;
	}

}
