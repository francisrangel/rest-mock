package restmock.request;

import java.util.HashMap;
import java.util.Map;

import restmock.response.Response;

public class RouteManager {

	private static RouteManager instance;
	private Map<Route, Response> routes = new HashMap<Route, Response>();
	
	private RouteManager() { }
	
	public static RouteManager getInstance() {
		if (instance == null) instance = new RouteManager();
		return instance;
	}

	public void registerRoute(Route route, Response response) {
		routes.put(route, response);
	}

	public Response get(Route route) {
		return routes.get(route);
	}
	
	public void clean() {
		routes = new HashMap<Route, Response>();
	}

}
