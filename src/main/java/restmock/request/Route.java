package restmock.request;

public class Route {
	
	private HttpMethod method;
	private String uri;
	
	public Route(HttpMethod method, String uri) {
		this.method = method;
		this.uri = uri;
	}
	
	public Route(String method, String uri) {
		this.method = HttpMethod.byString(method);
		this.uri = uri;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Route other = (Route) obj;
		if (method != other.method)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
}
