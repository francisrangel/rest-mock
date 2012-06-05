package restmock;

import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.response.Response;

public class HttpResponse implements RestMockResponse {

	private Route route;

	public HttpResponse(Route route) {
		this.route = route;
	}

	@Override
	public RestMockResponse thenReturn(Response body) {
		RouteManager.registerRoute(route, body);
		return this;
	}
	
}
