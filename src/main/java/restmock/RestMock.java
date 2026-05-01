package restmock;

import restmock.http.HttpMethod;
import restmock.routing.Route;
import restmock.routing.RouteManager;
import restmock.routing.RouteRegister;

public final class RestMock {

	public static final int DEFAULT_PORT = 9080;

	private static final RouteManager routeManager = new RouteManager();
	private static final RequestLog requestLog = new RequestLog();
	private static final RestMockServer server = new RestMockServer(routeManager, requestLog);

	private RestMock() {}

	public static RestMockResponse whenGet(String uri) {
		return registerRoute(HttpMethod.GET, uri);
	}

	public static RestMockResponse whenPost(String uri) {
		return registerRoute(HttpMethod.POST, uri);
	}

	public static RestMockResponse whenPut(String uri) {
		return registerRoute(HttpMethod.PUT, uri);
	}

	public static RestMockResponse whenDelete(String uri) {
		return registerRoute(HttpMethod.DELETE, uri);
	}

	public static RestMockResponse whenPatch(String uri) {
		return registerRoute(HttpMethod.PATCH, uri);
	}

	public static RestMockResponse whenHead(String uri) {
		return registerRoute(HttpMethod.HEAD, uri);
	}

	public static RestMockResponse whenOptions(String uri) {
		return registerRoute(HttpMethod.OPTIONS, uri);
	}

	public static RequestLog requests() {
		return requestLog;
	}

	private static RestMockResponse registerRoute(HttpMethod method, String uri) {
		return new RouteRegister(new Route(method, uri), routeManager);
	}

	public static void startServer() {
		startServer(DEFAULT_PORT);
	}

	public static void startServer(int port) {
		server.start(port);
	}

	public static void stopServer() {
		server.stop();
	}

	public static void clean() {
		routeManager.clean();
		requestLog.clear();
	}

	static RouteManager routeManager() {
		return routeManager;
	}

}
