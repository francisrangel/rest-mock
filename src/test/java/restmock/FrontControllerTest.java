package restmock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

import restmock.request.FrontController;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.response.TextPlain;

public class FrontControllerTest {
	
	private HttpServletRequest request = mock(HttpServletRequest.class);
	private HttpServletResponse response = mock(HttpServletResponse.class);
	private RouteManager routeManager = mock(RouteManager.class);
	
	@Test
	public void frontControllerShouldAsksRouteManagerAResponseToProcessARequest() throws ServletException, IOException {
		when(request.getMethod()).thenReturn("GET");
		
		FrontController controller = new FrontController();
		controller.processRequest(request, response, routeManager);
		
		verify(routeManager, times(1)).get(any(Route.class));
	}
	
	@Test
	public void frontControllerShouldReturn404WhenRouteManagerDontKnownARoute() throws ServletException, IOException {
		when(request.getMethod()).thenReturn("GET");
		when(routeManager.get(any(Route.class))).thenReturn(null);
		
		FrontController controller = new FrontController();
		controller.processRequest(request, response, routeManager);
		
		verify(response).setStatus(404);
	}
	
	@Test
	public void frontControllerShouldReturn200WhenRouteManagerKnownARoute() throws ServletException, IOException {
		PrintWriter printWriter = mock(PrintWriter.class);
		doNothing().when(printWriter).println(Mockito.anyString());
		
		when(request.getMethod()).thenReturn("GET");
		when(routeManager.get(any(Route.class))).thenReturn(new TextPlain("ok"));
		when(response.getWriter()).thenReturn(printWriter);
		
		FrontController controller = new FrontController();
		controller.processRequest(request, response, routeManager);
		
		verify(response).setStatus(200);
	}

}
