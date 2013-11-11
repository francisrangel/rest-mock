package restmock;

import restmock.request.HttpMethod;
import restmock.request.Route;
import restmock.request.RouteRegister;

public class RestMock {
	
	public static final int DEFAULT_PORT = 9080;
	
	private static final RestMockServer server = new RestMockServer();

	public static RestMockResponse whenGet(String uri) {
		return registerRoute(HttpMethod.GET, uri);
	}

	public static RestMockResponse whenPost(String uri) {
		return registerRoute(HttpMethod.POST, uri);
	}
	
	private static RestMockResponse registerRoute(HttpMethod method, String uri) {
		return new RouteRegister(new Route(method, uri));
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
		server.clean();
	}

}
