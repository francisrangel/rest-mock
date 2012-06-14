package restmock.request;

import restmock.RestMockResponse;
import restmock.response.Response;

public class RouteRegister implements RestMockResponse {

	private final Route route;

	public RouteRegister(Route route) {
		this.route = route;
	}

	@Override
	public void thenReturn(Response body) {
		RouteManager.getInstance().registerRoute(route, body);
	}
	
}
