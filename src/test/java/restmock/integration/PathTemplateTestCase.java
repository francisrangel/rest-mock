package restmock.integration;

import org.junit.Test;

import restmock.RestMock;
import restmock.request.HttpMethod;

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

		java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
			.uri(java.net.URI.create(baseUrl + "/users/1/extra"))
			.GET()
			.build();
		java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

		org.junit.Assert.assertEquals(404, response.statusCode());
	}

	@Test
	public void pathCapturesCombineWithQueryParameters() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id} aka ${nickname}");

		requestMethodWithResultString(baseUrl + "/users/42?nickname=bob", "user 42 aka bob", HttpMethod.GET);
	}

}
