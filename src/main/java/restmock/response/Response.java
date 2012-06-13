package restmock.response;

import restmock.response.visitor.Visitable;
import restmock.response.visitor.Visitor;

public abstract class Response implements Visitable<Response> {
	
	private String content;
	
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
	
	@Override
	public String toString() {
		return content;
	}
	
	public void accept(Visitor<Response> visitor) {
		visitor.visit(this);
	}

}
