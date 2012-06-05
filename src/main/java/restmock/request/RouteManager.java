package restmock.request;

import java.util.HashMap;
import java.util.Map;

import restmock.response.Response;

public class RouteManager {

	private static Map<Route, Response> routes = new HashMap<Route, Response>();

	public static void registerRoute(Route route, Response response) {
		routes.put(route, response);
	}

	public static Response get(Route route) {
		return routes.get(route);
	}
	
	public static void clean() {
		routes = new HashMap<Route, Response>();
	}

}
