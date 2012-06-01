package br.com.frs.server;

public class MediaType {
	
	public static MediaType TEXT_HTML = new MediaType("text/html");
	public static MediaType TEXT_PLAIN = new MediaType("text/plain");
	public static MediaType APPLICATION_JSON = new MediaType("application/json");
	
	private String type;
	
	MediaType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

}
