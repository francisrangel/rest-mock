package restmock.integration;

import static org.junit.Assert.assertEquals;
import static restmock.utils.Resource.LINE_SEPARATOR;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.junit.Test;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class MockHttpErrorsTestCase extends IntegrationTestBase {
	
	@Test
	public void returningError404() throws Exception {
		RestMock.whenGet("/test").thenReturnText("Hello World!");
		RestMock.startServer();
		
		ContentExchange exchange = sendRequestAndWaitForDone(baseUrl + "/test1", HttpMethod.GET);
		
		assertEquals(HttpServletResponse.SC_NOT_FOUND, exchange.getResponseStatus());
	}
	
	@Test
	public void returningBadRequestForGETMethod() throws Exception {
		RestMock.whenGet("/test").thenReturnErrorCodeWithMessage(HttpServletResponse.SC_BAD_REQUEST, "Message for error 500 GET");
		RestMock.startServer();
		
		ContentExchange exchange = sendRequestAndWaitForDone(baseUrl + "/test", HttpMethod.GET);
		
		assertEquals(HttpServletResponse.SC_BAD_REQUEST, exchange.getResponseStatus());
		assertEquals("Message for error 500 GET" + LINE_SEPARATOR, exchange.getResponseContent());
	}
	
	@Test
	public void returningBadRequestForPOSTMethod() throws Exception {
		RestMock.whenPost("/test").thenReturnErrorCodeWithMessage(HttpServletResponse.SC_BAD_REQUEST, "Message for error 500 POST");
		RestMock.startServer();
		
		ContentExchange exchange = sendRequestAndWaitForDone(baseUrl + "/test", HttpMethod.POST);
		
		assertEquals(HttpServletResponse.SC_BAD_REQUEST, exchange.getResponseStatus());
		assertEquals("Message for error 500 POST" + LINE_SEPARATOR, exchange.getResponseContent());
	}
	
	@Test
	public void returningForbiddenForGETMethod() throws Exception {
		RestMock.whenGet("/test").thenReturnErrorCodeWithMessage(HttpServletResponse.SC_FORBIDDEN, "Forbidden GET");
		RestMock.startServer();
		
		ContentExchange exchange = sendRequestAndWaitForDone(baseUrl + "/test", HttpMethod.GET);
		
		assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.getResponseStatus());
		assertEquals("Forbidden GET" + LINE_SEPARATOR, exchange.getResponseContent());
	}
	
	@Test
	public void returningForbiddenForPOSTMethod() throws Exception {
		RestMock.whenPost("/test").thenReturnErrorCodeWithMessage(HttpServletResponse.SC_FORBIDDEN, "Forbidden POST");
		RestMock.startServer();
		
		ContentExchange exchange = sendRequestAndWaitForDone(baseUrl + "/test", HttpMethod.POST);
		
		assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.getResponseStatus());
		assertEquals("Forbidden POST" + LINE_SEPARATOR, exchange.getResponseContent());
	}

}
