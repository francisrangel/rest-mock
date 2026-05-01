package restmock.response;

public final class Html extends Response {

	public Html(String body) {
		super(body);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_HTML;
	}

}
