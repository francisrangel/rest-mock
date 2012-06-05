package restmock;

import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.response.Response;

public class HttpResponse implements RestMockResponse {

	private final RouteManager routeManager;
	private final Route route;

	public HttpResponse(RouteManager routeManager, Route route) {
		this.routeManager = routeManager;
		this.route = route;
	}

	@Override
	public RestMockResponse thenReturn(Response body) {
		routeManager.registerRoute(route, body);
		return this;
	}
	
}
