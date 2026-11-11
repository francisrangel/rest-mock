package org.simulatest.restmock.internal.response;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A text body with {@code ${...}} placeholders, rendered per request from what
 * that request carried.
 */
public abstract class Template extends Response {

	private final String content;

	Template(String body, ContentType contentType) {
		super(contentType);
		this.content = body;
	}

	@Override
	public boolean isTextual() {
		return true;
	}

	@Override
	public byte[] render(Map<String, String> parameters) {
		return Substitution.fill(content, parameters, this::escape).getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Escapes a substituted value for this body's format. Plain text needs
	 * nothing; JSON, XML, and HTML override it so a value carrying a quote or an
	 * angle bracket cannot produce a malformed document.
	 */
	String escape(String value) {
		return value;
	}

	/** The body as written, placeholders and all. */
	public String getContent() {
		return content;
	}

	static String serialize(ObjectMapper mapper, Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

}
