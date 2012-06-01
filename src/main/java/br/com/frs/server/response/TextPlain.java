package br.com.frs.server.response;

import br.com.frs.server.MediaType;

public class TextPlain extends Response {

	public TextPlain(String body) {
		super(body);
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.TEXT_PLAIN;
	}

}