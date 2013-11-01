package restmock.integration;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.junit.Test;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class ServerCleanTest extends IntegrationTestBase {
	
	@Test
	public void routesShouldBeEliminatedOnClean() throws Exception {
		RestMock.whenGet("/test").thenReturnJSON("{ my: test }");		
		requestMethodWithResultString(baseUrl + "/test", "{ my: test }", HttpMethod.GET);
		
		RestMock.clean();
		
		ContentExchange exchange = sendRequestAndWaitForDone(baseUrl + "/test", HttpMethod.GET);		
		assertEquals(HttpServletResponse.SC_NOT_FOUND, exchange.getResponseStatus());
	}

}
