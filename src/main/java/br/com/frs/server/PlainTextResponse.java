package br.com.frs.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

public class PlainTextResponse extends AbstractHandler implements Reponse {

	private String desiredResponseValue;

	public PlainTextResponse(ContextHandler context) {
		context.setHandler(this);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException {
		baseRequest.setHandled(true);
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(desiredResponseValue);
	}

	@Override
	public void thenReturn(String desiredResponseValue) {
		this.desiredResponseValue = desiredResponseValue;
	}

}
