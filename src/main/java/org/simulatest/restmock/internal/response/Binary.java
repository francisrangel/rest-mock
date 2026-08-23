package org.simulatest.restmock.internal.response;

import java.util.Map;

public final class Binary extends Response {

	private final byte[] bytes;
	private final ContentType contentType;

	public Binary(byte[] bytes, ContentType contentType) {
		super("");
		this.bytes = bytes;
		this.contentType = contentType;
	}

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
