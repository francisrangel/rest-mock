package restmock;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import restmock.request.HttpMethod;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.request.RouteRegister;
import restmock.response.ContentType;
import restmock.response.Response;

public class HttpResponseForPOSTMethodTest {
	
	private RouteRegister subject;
	private Route route;
	
	@Before
	public void setUp() {
		route = new Route(HttpMethod.POST, "/teste");
		subject = new RouteRegister(route);
	}
	
	@Test
	public void testSimpleTextResponse() {
		subject.thenReturnText("Test succeed");
		
		Response response = RouteManager.getInstance().get(route);
		
		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Test succeed", response.getContent());
	}

}
