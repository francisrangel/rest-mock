package org.simulatest.restmock.internal.response;

import java.net.HttpURLConnection;

public final class NotConfigured extends Template {

	public NotConfigured(String uri) {
		super("Route registered for " + uri + " but no response was configured. Call thenReturn*() to set one.", ContentType.TEXT_PLAIN);
		setResponseStatus(HttpURLConnection.HTTP_NOT_IMPLEMENTED);
	}

}
