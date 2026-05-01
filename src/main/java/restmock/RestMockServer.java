package restmock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import restmock.http.FrontController;
import restmock.routing.RouteManager;

public class RestMockServer {

	private final RouteManager routeManager;
	private final RequestLog requestLog;
	private HttpServer server;

	protected RestMockServer(RouteManager routeManager, RequestLog requestLog) {
		this.routeManager = routeManager;
		this.requestLog = requestLog;
	}

	public void start(int port) {
		if (server != null) return;

		try {
			server = HttpServer.create(new InetSocketAddress(port), 0);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not start the server!", e);
		}

		server.createContext("/", new FrontController(routeManager, requestLog));
		server.setExecutor(null);
		server.start();
	}

	public void stop() {
		if (server == null) return;

		server.stop(0);
		server = null;
		routeManager.clean();
		requestLog.clear();
	}

}
