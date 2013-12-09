package restmock.request;

public class Route {
	
	private final HttpMethod method;
	private final String uri;
	
	public Route(HttpMethod method, String uri) {
		this.method = method;
		this.uri = uri;
	}
	
	public Route(String method, String uri) {
		this(HttpMethod.byString(method), uri);
	}
	
	public HttpMethod getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	@Override
	public int hashCode() {
		return 31 * method.hashCode() * uri.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		
		Route other = (Route) obj;
		return other.uri.equals(uri) && other.method.equals(method);
	}
	
}
