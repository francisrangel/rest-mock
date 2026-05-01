package restmock.response;

import java.net.HttpURLConnection;

public final class NotConfigured extends Response {

	public NotConfigured(String uri) {
		super("Route registered for " + uri + " but no response was configured. Call thenReturn*() to set one.");
		setResponseStatus(HttpURLConnection.HTTP_NOT_IMPLEMENTED);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_PLAIN;
	}

}
