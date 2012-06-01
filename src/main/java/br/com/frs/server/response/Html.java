package br.com.frs.server.response;

import br.com.frs.server.MediaType;

public class Html extends Response {

	public Html(String body) {
		super(body);
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.TEXT_HTML;
	}

}