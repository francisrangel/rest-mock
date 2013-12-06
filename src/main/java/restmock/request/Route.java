package restmock.request;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

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
		return reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return reflectionEquals(this, obj);
	}
	
}
