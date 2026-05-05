package restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import restmock.internal.response.ContentType;
import restmock.internal.response.Response;
import restmock.internal.routing.Route;
import restmock.internal.routing.RouteManager;
import restmock.internal.routing.RouteRegister;

public class HttpResponseForPOSTMethodTest {

	private RouteManager routeManager;
	private RouteRegister subject;
	private Route route;

	@BeforeEach
	public void setUp() {
		routeManager = new RouteManager();
		route = new Route(HttpMethod.POST, "/teste");
		subject = new RouteRegister(route, routeManager);
	}

	@Test
	public void testSimpleTextResponse() {
		subject.thenReturnText("Test succeed");

		Response response = routeManager.get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Test succeed", response.getContent());
	}

}
