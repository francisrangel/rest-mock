package restmock.response;

import restmock.response.visitor.ReplacerParametersVisitor;

public abstract class Response {
	
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
	
	public void accept(ReplacerParametersVisitor visitor) {
		visitor.visit(this);
	}

}
