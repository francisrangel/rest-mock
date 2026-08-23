package org.simulatest.restmock.internal.response;

import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Response {

	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private final String content;
	private final Map<String, String> header;
	private int responseStatus = HttpURLConnection.HTTP_OK;
	private long delayMillis;

	Response(String body) {
		this.content = body;
		this.header = new HashMap<>();
	}

	public abstract ContentType getContentType();

	/** True when the body is text, and so safe to preview in logs. */
	public boolean isTextual() {
		return true;
	}

	/** The bytes to write for this response, with {@code ${...}} placeholders substituted. */
	public byte[] render(Map<String, String> parameters) {
		String rendered = PARAMETER_PATTERN.matcher(content).replaceAll(match -> {
			String key = match.group(1);
			return Matcher.quoteReplacement(parameters.getOrDefault(key, match.group(0)));
		});
		return rendered.getBytes(StandardCharsets.UTF_8);
	}

	public String getContent() {
		return content;
	}

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	public long getDelayMillis() {
		return delayMillis;
	}

	public void setDelayMillis(long delayMillis) {
		this.delayMillis = delayMillis;
	}

	public Map<String, String> getHeader() {
		return Collections.unmodifiableMap(header);
	}

	public void addHeader(String property, String value) {
		header.put(property, value);
	}

	@Override
	public String toString() {
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
