package restmock.response;

public class ContentType {
	
	public static ContentType TEXT_HTML = new ContentType("text/html");
	public static ContentType TEXT_PLAIN = new ContentType("text/plain");
	public static ContentType APPLICATION_JSON = new ContentType("application/json");
	
	private String type;
	
	ContentType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

}
