package restmock.response;

public final class TextPlain extends Response {

	public TextPlain(String body) {
		super(body);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_PLAIN;
	}

}
