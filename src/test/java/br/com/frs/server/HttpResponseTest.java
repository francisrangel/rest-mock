package br.com.frs.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.Before;
import org.junit.Test;

import br.com.frs.server.response.JSON;
import br.com.frs.server.response.Html;
import br.com.frs.server.response.TextPlain;

public class HttpResponseTest {

	private Request baseRequest;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private PrintWriter printWriter;
	private ContextHandler context;
	private HttpResponse subject;

	@Before
	public void setUp() throws IOException {
		context = mock(ContextHandler.class);
		baseRequest = mock(Request.class);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		printWriter = mock(PrintWriter.class);

		when(response.getWriter()).thenReturn(printWriter);

		subject = new HttpResponse(context);
	}

	@Test
	public void testPlainTextResponse() throws Exception {
		subject.thenReturn(new TextPlain("Hello World!"));
		subject.handle("", baseRequest, request, response);

		verify(baseRequest).setHandled(true);
		verify(response).setContentType("text/plain");
		verify(response).setStatus(HttpServletResponse.SC_OK);
		verify(response).getWriter();
		verify(printWriter).println("Hello World!");
	}

	@Test
	public void testHtmlResponse() throws Exception {
		subject.thenReturn(new Html("<h1>Mock rules</h1>"));
		subject.handle("", baseRequest, request, response);

		verify(response).setContentType("text/html");
		verify(printWriter).println("<h1>Mock rules</h1>");
	}
	
	@Test
	public void testJSONResponse() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		
		subject.thenReturn(new JSON(simpleJSON));
		subject.handle("", baseRequest, request, response);

		verify(response).setContentType("application/json");
		verify(printWriter).println(simpleJSON);
	}
	
	@Test
	public void testJSONObjectResponse() throws Exception {
		class Developer {
			private String name;
			private int age;
			
			Developer(String name, int age) {
				this.name = name;
				this.age = age;
			}

			@SuppressWarnings("unused")
			public String getName() {
				return name;
			}

			@SuppressWarnings("unused")
			public int getAge() {
				return age;
			}
		}
		
		subject.thenReturn(new JSON(new Developer("Bob", 25)));
		subject.handle("", baseRequest, request, response);
		
		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";

		verify(response).setContentType("application/json");
		verify(printWriter).println(expectedJSON);
	}
	
}
