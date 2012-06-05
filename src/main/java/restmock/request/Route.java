package restmock.request;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	
}
