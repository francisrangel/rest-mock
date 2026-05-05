package org.simulatest.restmock.internal.response;

public record ContentType(String value) {

	public static final ContentType TEXT_HTML = new ContentType("text/html");
	public static final ContentType TEXT_PLAIN = new ContentType("text/plain");
	public static final ContentType TEXT_XML = new ContentType("text/xml");
	public static final ContentType APPLICATION_JSON = new ContentType("application/json");
	public static final ContentType APPLICATION_FORM_URLENCODED = new ContentType("application/x-www-form-urlencoded");
	public static final ContentType APPLICATION_OCTET_STREAM = new ContentType("application/octet-stream");

	public static ContentType of(String value) {
		return new ContentType(value);
	}

	public String getType() {
		return value;
	}

}
