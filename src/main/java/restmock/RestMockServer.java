package restmock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import restmock.request.FrontController;
import restmock.request.RouteManager;

public class RestMockServer {

	private HttpServer server;

	protected RestMockServer() {
	}

	public void start(int port) {
		if (server != null) return;

		try {
			server = HttpServer.create(new InetSocketAddress(port), 0);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not start the server!", e);
		}

		server.createContext("/", new FrontController());
		server.setExecutor(null);
		server.start();
	}

	public void stop() {
		if (server == null) return;

		server.stop(0);
		server = null;
		clean();
	}

	public void clean() {
		RouteManager.getInstance().clean();
	}

}
