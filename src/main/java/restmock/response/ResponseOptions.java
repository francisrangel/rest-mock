package restmock.response;

import restmock.request.Route;
import restmock.request.RouteManager;

public class ResponseOptions {
	
	private final Route route;

	public ResponseOptions(Route route) {
		this.route = route;
	}
	
	public ResponseOptions withHeader(String property, String value) {
		Response response = RouteManager.getInstance().get(route);
		response.addHeader(property, value);
		return this;
	}

}
