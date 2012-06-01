package br.com.frs.server.response;

import br.com.frs.server.MediaType;

public abstract class Response {
	
	private String content;
	
	Response(String body) {
		this.content = body;
	}

	public abstract MediaType getMediaType();

	public String getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		return content;
	}

}
