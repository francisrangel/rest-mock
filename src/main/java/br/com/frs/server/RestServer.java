package br.com.frs.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;

public class RestServer {

	private Server server;

	public RestServer(int port) {
		server = new Server(port);
	}

	public Reponse when(String path) {
		ContextHandler context = new ContextHandler();
		context.setContextPath(path);
		context.setResourceBase(".");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());

		server.setHandler(context);

		return new PlainTextResponse(context);
	}

	public void start() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					server.start();
					server.join();
				} catch (Exception e) {
					throw new RuntimeException("Could not start the server!", e);
				}
			}
		};

		Thread thread = new Thread(runnable);
		thread.setDaemon(true);
		thread.start();
	}

	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			throw new RuntimeException("Could not stop the server!", e);
		}
	}

}
