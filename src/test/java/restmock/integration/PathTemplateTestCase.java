package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;

import org.junit.Test;

import restmock.RestMock;
import restmock.http.HttpMethod;

public class PathTemplateTestCase extends IntegrationTestBase {

	@Test
	public void singleSegmentCapture() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");

		requestMethodWithResultString(baseUrl + "/users/42", "user 42", HttpMethod.GET);
	}

	@Test
	public void multipleSegmentCaptures() throws Exception {
		RestMock.whenGet("/users/{userId}/posts/{postId}").thenReturnText("u=${userId} p=${postId}");

		requestMethodWithResultString(baseUrl + "/users/7/posts/99", "u=7 p=99", HttpMethod.GET);
	}

	@Test
	public void literalPathBeatsTemplateForSamePath() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");
		RestMock.whenGet("/users/me").thenReturnText("you");

		requestMethodWithResultString(baseUrl + "/users/me", "you", HttpMethod.GET);
		requestMethodWithResultString(baseUrl + "/users/42", "user 42", HttpMethod.GET);
	}

	@Test
	public void registrationOrderDoesNotMatterForSpecificity() throws Exception {
		RestMock.whenGet("/users/me").thenReturnText("you");
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");

		requestMethodWithResultString(baseUrl + "/users/me", "you", HttpMethod.GET);
	}

	@Test
	public void templateDoesNotSpanSlash() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("matched");

		HttpResponse<String> response = sendRequest(baseUrl + "/users/1/extra", HttpMethod.GET);

		assertEquals(404, response.statusCode());
	}

	@Test
	public void pathCapturesCombineWithQueryParameters() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id} aka ${nickname}");

		requestMethodWithResultString(baseUrl + "/users/42?nickname=bob", "user 42 aka bob", HttpMethod.GET);
	}

}
