package org.simulatest.restmock.internal.response;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JSON extends Response {

	public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

	public JSON(String body) {
		super(body);
	}

	public JSON(Object object) {
		super(serialize(MAPPER, object));
	}

	@Override
	String escape(String value) {
		return Escape.json(value);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.APPLICATION_JSON;
	}

}
