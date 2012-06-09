package restmock.request;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import restmock.response.Response;
import restmock.response.visitor.ReplacerParametersVisitor;

public class FrontController extends HttpServlet {

	private static final long serialVersionUID = -8762086840436163410L;
	
	public void processRequest(HttpServletRequest request, HttpServletResponse response, RouteManager routeManager) throws ServletException, IOException {
		Route route = new Route(request.getMethod(), request.getRequestURI());
		Response content = routeManager.get(route);
		
		if (content == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		new ReplacerParametersVisitor(request).visit(content);
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(content.getContentType().getType());
		response.getWriter().println(content.getContent());
		
		allowCrossDomainAccess(response);
	}
	
	private void allowCrossDomainAccess(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
		response.setHeader("Access-Control-Max-Age", "360");
		response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
		response.setHeader("Access-Control-Allow-Credentials", "true");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, RouteManager.getInstance());
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, RouteManager.getInstance());
	}

}
