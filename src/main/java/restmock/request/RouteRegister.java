package restmock.request;

import restmock.RestMockResponse;
import restmock.response.Response;

public class RouteRegister implements RestMockResponse {

	private final RouteManager routeManager;
	private final Route route;

	public RouteRegister(RouteManager routeManager, Route route) {
		this.routeManager = routeManager;
		this.route = route;
	}

	@Override
	public RestMockResponse thenReturn(Response body) {
		routeManager.registerRoute(route, body);
		return this;
	}
	
}
