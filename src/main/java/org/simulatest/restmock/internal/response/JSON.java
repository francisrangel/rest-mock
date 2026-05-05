package org.simulatest.restmock.internal.response;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JSON extends Response {

	public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

	public JSON(String body) {
		super(body);
	}

	public JSON(Object object) {
		super(write(object));
	}

	@Override
	public ContentType getContentType() {
		return ContentType.APPLICATION_JSON;
	}

	private static String write(Object object) {
		try {
			return MAPPER.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

}
