package br.com.frs.server.response;

public abstract class Response {
	
	private String content;
	
	Response(String body) {
		this.content = body;
	}

	public abstract ContentType getContentType();

	public String getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		return content;
	}

}
