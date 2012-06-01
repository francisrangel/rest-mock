package br.com.frs.server.response;

import br.com.frs.server.MediaType;

public class JSON extends Response {

	public JSON(String body) {
		super(body);
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.APPLICATION_JSON;
	}

}
