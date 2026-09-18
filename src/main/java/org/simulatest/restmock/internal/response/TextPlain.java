package org.simulatest.restmock.internal.response;

public final class TextPlain extends Template {

	public TextPlain(String body) {
		super(body, ContentType.TEXT_PLAIN);
	}

}
