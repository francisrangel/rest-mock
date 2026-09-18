package org.simulatest.restmock.internal.response;

public final class Html extends Template {

	public Html(String body) {
		super(body, ContentType.TEXT_HTML);
	}

	@Override
	String escape(String value) {
		return Escape.markup(value);
	}

}
