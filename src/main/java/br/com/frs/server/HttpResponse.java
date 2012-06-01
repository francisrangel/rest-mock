package br.com.frs.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import br.com.frs.server.response.Response;

public class HttpResponse extends AbstractHandler implements RestMockResponse {

	private Response data;

	public HttpResponse(ContextHandler context) {
		context.setHandler(this);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		
		response.setContentType(data.getContentType().getType());
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(data.getContent());
		
		allowCrossDomainAccess(response);
	}

	private void allowCrossDomainAccess(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
		response.setHeader("Access-Control-Max-Age", "360");
		response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
		response.setHeader("Access-Control-Allow-Credentials", "true");
	}

	@Override
	public RestMockResponse thenReturn(Response body) {
		this.data = body;
		return this;
	}
	
}
