package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import restmock.RestMock;
import restmock.http.HttpHeader;
import restmock.http.HttpMethod;

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

		String allow = response.headers().firstValue(HttpHeader.ALLOW).orElse("");
		Set<String> methods = Set.of(allow.split(",\\s*"));
		assertEquals(Set.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.OPTIONS.name()), methods);
	}

	@Test
	public void head() throws Exception {
		RestMock.whenHead("/test").thenReturnText("Head succeed");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.HEAD);

		assertEquals(200, response.statusCode());
		assertEquals("", response.body());

		String expectedLength = Integer.toString(("Head succeed" + System.lineSeparator()).getBytes().length);
		assertEquals(expectedLength, response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElse(""));
	}

	@Test
	public void corsAllowMethodsHeaderListsAllSupportedVerbs() throws Exception {
		RestMock.whenGet("/test").thenReturnText("ok");

		HttpResponse<String> response = sendRequest(baseUrl + "/test", HttpMethod.GET);

		String allMethods = Arrays.stream(HttpMethod.values())
			.map(Enum::name)
			.collect(Collectors.joining(", "));

		assertEquals(
			allMethods,
			response.headers().firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS).orElse(""));
	}

}
