package restmock.response;

import javax.servlet.http.HttpServletResponse;

import restmock.response.visitor.Visitable;
import restmock.response.visitor.Visitor;

public abstract class Response implements Visitable<Response> {
	
	private String content;
	private Integer responseStatus;
	
	Response(String body) {
		this.content = body;
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
	
	@Override
	public String toString() {
		return content;
	}
	
	public void accept(Visitor<Response> visitor) {
		visitor.visit(this);
	}

}
