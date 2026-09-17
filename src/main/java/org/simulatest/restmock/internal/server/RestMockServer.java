package org.simulatest.restmock.internal.server;

import static java.lang.System.Logger.Level.INFO;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** The lifecycle of one JDK HTTP server: bind, hand every request to a handler, stop. */
public class RestMockServer {

	private static final System.Logger log = System.getLogger(RestMockServer.class.getName());

	/** Reported by {@link #port()} when the server is not running. */
	public static final int NOT_RUNNING = -1;

	private final HttpHandler handler;
	private HttpServer server;
	private ExecutorService workers;

	public RestMockServer(HttpHandler handler) {
		this.handler = handler;
	}

	/**
	 * Starting an already-running server is a no-op, but only when the port
	 * agrees. Silently ignoring a different port would leave the caller's
	 * requests going to a port nothing listens on, with nothing to explain it.
	 *
	 * Port 0 means "any free port", so it never conflicts.
	 */
	public void start(int port) {
		if (server != null) {
			if (port != 0 && port != port()) {
				throw new IllegalStateException(
					"RestMock is already running on port " + port() + ", so it cannot start on port " + port
						+ ". Stop it first, or use the port it is on.");
			}
			return;
		}

		// Loopback only: a stub is never reachable from another machine, the OS
		// firewall has nothing to ask about, and the address agrees with the
		// localhost URL that baseUrl() reports.
		try {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not bind RestMock to port " + port, e);
		}

		workers = Executors.newCachedThreadPool(new WorkerFactory());

		server.createContext("/", handler);
		server.setExecutor(workers);
		server.start();

		log.log(INFO, () -> "RestMock server started on port " + port());
	}

	public boolean isRunning() {
		return server != null;
	}

	/** The bound port, or {@link #NOT_RUNNING}. Resolves the real port when started on 0. */
	public int port() {
		return isRunning() ? server.getAddress().getPort() : NOT_RUNNING;
	}

	public void stop() {
		if (server == null) return;

		int port = port();

		server.stop(0);
		workers.shutdownNow();
		server = null;
		workers = null;

		log.log(INFO, () -> "RestMock server stopped on port " + port);
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
