package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Test;

import restmock.RestMock;

public class JsonBodyTestCase extends IntegrationTestBase {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	@Test
	public void flatJsonKey() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		postJson("/test", "{\"name\":\"Bob\"}", "hello Bob");
	}

	@Test
	public void nestedJsonKey() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${user.name}");

		postJson("/test", "{\"user\":{\"name\":\"Bob\"}}", "hello Bob");
	}

	@Test
	public void arrayIndexInJson() throws Exception {
		RestMock.whenPost("/test").thenReturnText("first=${items.0.x}");

		postJson("/test", "{\"items\":[{\"x\":\"a\"},{\"x\":\"b\"}]}", "first=a");
	}

	@Test
	public void numericAndBooleanScalars() throws Exception {
		RestMock.whenPost("/test").thenReturnText("age=${age} active=${active}");

		postJson("/test", "{\"age\":25,\"active\":true}", "age=25 active=true");
	}

	@Test
	public void nonJsonContentTypeLeavesPlaceholderLiteral() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + "/test"))
			.header("Content-Type", "text/plain")
			.POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Bob\"}"))
			.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals("hello ${name}" + LINE_SEPARATOR, response.body());
	}

	@Test
	public void malformedJsonIsIgnoredAndRouteStillResponds() throws Exception {
		RestMock.whenPost("/test").thenReturnText("hello ${name}");

		postJson("/test", "{not json", "hello ${name}");
	}

	private void postJson(String path, String jsonBody, String expectedAnswer) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + path))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
			.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals(expectedAnswer + LINE_SEPARATOR, response.body());
	}

}
