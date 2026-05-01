package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.Set;

import org.junit.Test;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class WhenOtherMethodsTestCase extends IntegrationTestBase {

	@Test
	public void put() throws Exception {
		RestMock.whenPut("/test").thenReturnText("Put succeed");

		requestMethodWithResultString(baseUrl + "/test", "Put succeed", HttpMethod.PUT);
	}

	@Test
	public void delete() throws Exception {
		RestMock.whenDelete("/test").thenReturnText("Delete succeed");

		requestMethodWithResultString(baseUrl + "/test", "Delete succeed", HttpMethod.DELETE);
	}

	@Test
	public void patch() throws Exception {
		RestMock.whenPatch("/test").thenReturnText("Patch succeed");

		requestMethodWithResultString(baseUrl + "/test", "Patch succeed", HttpMethod.PATCH);
	}

	@Test
	public void options() throws Exception {
		RestMock.whenOptions("/test").thenReturnText("Options succeed");

		requestMethodWithResultString(baseUrl + "/test", "Options succeed", HttpMethod.OPTIONS);
	}

	@Test
	public void optionsAllowHeaderListsRegisteredMethodsForPath() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");
		RestMock.whenPost("/test").thenReturnText("ok");
		RestMock.whenOptions("/test").thenReturnText("ok");
		RestMock.whenGet("/other").thenReturnText("ok");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.OPTIONS);

		String allow = response.headers().firstValue("Allow").orElse("");
		Set<String> methods = Set.of(allow.split(",\\s*"));
		assertEquals(Set.of("GET", "POST", "OPTIONS"), methods);
	}

	@Test
	public void head() throws Exception {
		RestMock.whenHead("/test").thenReturnText("Head succeed");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.HEAD);

		assertEquals(200, response.statusCode());
		assertEquals("", response.body());

		String expectedLength = Integer.toString(("Head succeed" + System.lineSeparator()).getBytes().length);
		assertEquals(expectedLength, response.headers().firstValue("Content-Length").orElse(""));
	}

	@Test
	public void corsAllowMethodsHeaderListsAllSupportedVerbs() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.GET);

		assertEquals(
			"GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS",
			response.headers().firstValue("Access-Control-Allow-Methods").orElse(""));
	}

}
