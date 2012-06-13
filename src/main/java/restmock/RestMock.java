package restmock;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import restmock.request.FrontController;
import restmock.request.HttpMethod;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.request.RouteRegister;

public class RestMock {

	private final Server server;
	private final RouteManager routeManager;
	
	public RestMock() {
		this(8080);
	}

	public RestMock(int port) {
		server = new Server(port);
		routeManager = RouteManager.getInstance();
	}

	public RestMockResponse whenGet(String uri) {
		return new RouteRegister(routeManager, new Route(HttpMethod.GET, uri));
	}
	
	public RestMockResponse whenPost(String uri) {
		return new RouteRegister(routeManager, new Route(HttpMethod.POST, uri));
	}

	public void startServer() {
		initContext();
		
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

	private void initContext() {
		ServletContextHandler context = new ServletContextHandler();
		
		context.setContextPath("/");
		context.setResourceBase(".");
		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		context.addServlet(FrontController.class, "/");
		
		server.setHandler(context);
	}

	public void stopServer() {
		try {
			server.stop();
			routeManager.clean();
		} catch (Exception e) {
			throw new RuntimeException("Could not stop the server!", e);
		}
	}
	
}
