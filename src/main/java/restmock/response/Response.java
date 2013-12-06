package restmock.response;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import restmock.response.visitor.Visitable;
import restmock.response.visitor.Visitor;

public abstract class Response implements Visitable<Response> {

	private String content;
	private Map<String, String> header;
	private Integer responseStatus;

	Response(String body) {
		this.content = body;
		this.header = new HashMap<String, String>();
	}

	public abstract ContentType getContentType();

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getResponseStatus() {
		return responseStatus != null ? responseStatus : HttpServletResponse.SC_OK;
	}

	public void setResponseStatus(Integer responseStatus) {
		this.responseStatus = responseStatus;
	}

	public Map<String, String> getHeader() {
		return header;
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
