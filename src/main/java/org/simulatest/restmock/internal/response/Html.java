package org.simulatest.restmock.internal.response;

public final class Html extends Template {

	public Html(String body) {
		super(body);
	}

	@Override
	String escape(String value) {
		return Escape.markup(value);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_HTML;
	}

}
