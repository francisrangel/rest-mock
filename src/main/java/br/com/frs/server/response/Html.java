package br.com.frs.server.response;

public class Html extends Response {

	public Html(String body) {
		super(body);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_HTML;
	}

}
