package br.com.frs.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

public class HttpResponse extends AbstractHandler implements Response {

	private String body;
	private MediaType type = MediaType.TEXT_HTML;

	public HttpResponse(ContextHandler context) {
		context.setHandler(this);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		
		response.setContentType(type.getType());
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(body);
	}

	@Override
	public Response thenReturn(String body) {
		this.body = body;
		return this;
	}
	
	@Override
	public Response withType(MediaType type) {
		this.type = type;
		return this;
	}
	
}
