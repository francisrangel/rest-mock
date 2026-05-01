package restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import restmock.http.HttpMethod;
import restmock.routing.Route;
import restmock.routing.RouteManager;
import restmock.request.RouteRegister;
import restmock.response.ContentType;
import restmock.response.Response;

public class HttpResponseForPOSTMethodTest {

	private RouteRegister subject;
	private Route route;

	@BeforeEach
	public void setUp() {
		route = new Route(HttpMethod.POST, "/teste");
		subject = new RouteRegister(route);
	}

	@AfterEach
	public void cleanUp() {
		RouteManager.getInstance().clean();
	}

	@Test
	public void testSimpleTextResponse() {
		subject.thenReturnText("Test succeed");

		Response response = RouteManager.getInstance().get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Test succeed", response.getContent());
	}

}
