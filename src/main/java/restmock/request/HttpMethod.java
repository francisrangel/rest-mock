package restmock.request;

public enum HttpMethod {
	
	GET, POST, PUT, DELETE;
	
	public static HttpMethod byString(String method) {
		for (HttpMethod value : HttpMethod.values())
			if (value.toString().equalsIgnoreCase(method)) 
				return value;
		
		throw new IllegalArgumentException(method + " isn't a HttpMethod");
	}

}
