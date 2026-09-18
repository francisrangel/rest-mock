package org.simulatest.restmock.internal.response;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JSON extends Template {

	public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

	public JSON(String body) {
		super(body, ContentType.APPLICATION_JSON);
	}

	public JSON(Object object) {
		this(serialize(MAPPER, object));
	}

	@Override
	String escape(String value) {
		return Escape.json(value);
	}

}
