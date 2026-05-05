package org.simulatest.restmock.internal.response;

public final class Binary extends Response {

	private final byte[] bytes;
	private final ContentType contentType;

	public Binary(byte[] bytes, ContentType contentType) {
		super("");
		this.bytes = bytes;
		this.contentType = contentType;
	}

	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public ContentType getContentType() {
		return contentType;
	}

}
