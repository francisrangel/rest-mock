package restmock.response;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import restmock.response.visitor.Visitable;
import restmock.response.visitor.Visitor;

public abstract class Response implements Visitable<Response> {

	private String content;
	private final Map<String, String> header;
	private int responseStatus = HttpURLConnection.HTTP_OK;

	Response(String body) {
		this.content = body;
		this.header = new HashMap<>();
	}

	public abstract ContentType getContentType();

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
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

	public void accept(Visitor<Response> visitor) {
		visitor.visit(this);
	}

}
