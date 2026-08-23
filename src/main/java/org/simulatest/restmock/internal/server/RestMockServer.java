package org.simulatest.restmock.internal.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import org.simulatest.restmock.RequestLog;
import org.simulatest.restmock.internal.http.FrontController;
import org.simulatest.restmock.internal.routing.RouteManager;

public class RestMockServer {

	private static final Logger log = LoggerFactory.getLogger(RestMockServer.class);

	private final RouteManager routeManager;
	private final RequestLog requestLog;
	private HttpServer server;
	private ExecutorService workers;

	public RestMockServer(RouteManager routeManager, RequestLog requestLog) {
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

		workers = Executors.newCachedThreadPool(new WorkerFactory());

		server.createContext("/", new FrontController(routeManager, requestLog));
		server.setExecutor(workers);
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
		workers.shutdownNow();
		server = null;
		workers = null;
		routeManager.clean();
		requestLog.clear();

		log.info("RestMock server stopped (cleared {} routes, {} requests)", routeCount, requestCount);
	}

	/**
	 * Daemon workers, so a test that forgets to stop the server never holds the
	 * JVM open. Named so a thread dump during a hung test points here.
	 */
	private static final class WorkerFactory implements ThreadFactory {

		private final AtomicInteger counter = new AtomicInteger();

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "restmock-" + counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}

	}

}
