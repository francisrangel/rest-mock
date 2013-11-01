package restmock;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import restmock.request.FrontController;
import restmock.request.RouteManager;

public class RestMockServer {

	private Server server;
	private boolean started;

	protected RestMockServer() {
	}

	public void start(int port) {
		if (started) return;
		initContext(port);

		try {
			server.start();
		} catch (Exception e) {
			throw new RuntimeException("Could not start the server!", e);
		}
		
		started = true;
	}

	private void initContext(int port) {
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler();

		context.setContextPath("/");
		context.setResourceBase(".");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		context.addServlet(FrontController.class, "/*");

		server.setHandler(context);
	}

	public void stop() {
		try {
			server.stop();
			clean();
		} catch (Exception e) {
			throw new RuntimeException("Could not stop the server!", e);
		}
		
		started = false;
	}

	public void clean() {
		RouteManager.getInstance().clean();		
	}

}
