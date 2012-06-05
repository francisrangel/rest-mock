package restmock;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import restmock.request.HttpMethod;
import restmock.request.Route;
import restmock.request.RouteManager;
import restmock.response.ContentType;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.Response;
import restmock.response.TextPlain;

public class HttpResponseTest {

	private HttpResponse subject;
	private Route route;

	@Before
	public void setUp() {
		route = new Route(HttpMethod.GET, "/teste/");
		subject = new HttpResponse(route);
	}

	@Test
	public void testPlainTextResponse() throws Exception {
		subject.thenReturn(new TextPlain("Hello World!"));
		
		Response response = RouteManager.get(route);

		assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
		assertEquals("Hello World!", response.getContent());
	}

	@Test
	public void testHtmlResponse() throws Exception {
		subject.thenReturn(new Html("<h1>Mock rules</h1>"));
		
		Response response = RouteManager.get(route);

		assertEquals(ContentType.TEXT_HTML, response.getContentType());
		assertEquals("<h1>Mock rules</h1>", response.getContent());
	}
	
	@Test
	public void testJSONResponse() throws Exception {
		String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";
		subject.thenReturn(new JSON(simpleJSON));
		
		Response response = RouteManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(simpleJSON, response.getContent());
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
		
		String expectedJSON = "{\"name\":\"Bob\",\"age\":25}";
		Response response = RouteManager.get(route);

		assertEquals(ContentType.APPLICATION_JSON, response.getContentType());
		assertEquals(expectedJSON, response.getContent());
	}
	
}
