package restmock.response;

public enum ContentType {

	TEXT_HTML("text/html"),
	TEXT_PLAIN("text/plain"),
	TEXT_XML("text/xml"),
	APPLICATION_JSON("application/json");

	private final String type;

	ContentType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

}
