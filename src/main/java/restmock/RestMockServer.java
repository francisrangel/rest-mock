package restmock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import restmock.http.FrontController;
import restmock.routing.RouteManager;

public class RestMockServer {

	private static final Logger log = LoggerFactory.getLogger(RestMockServer.class);

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
			log.error("Failed to bind RestMock to port {}: {}", port, e.getMessage());
			throw new UncheckedIOException("Could not start the server!", e);
		}

		server.createContext("/", new FrontController(routeManager, requestLog));
		server.setExecutor(null);
		server.start();

		log.info("RestMock server started on port {}", port);
	}

	public void stop() {
		if (server == null) {
			log.debug("stop() called but server is not running");
			return;
		}

		int routeCount = routeManager.size();
		int requestCount = requestLog.count();

		server.stop(0);
		server = null;
		routeManager.clean();
		requestLog.clear();

		log.info("RestMock server stopped (cleared {} routes, {} requests)", routeCount, requestCount);
	}

}
