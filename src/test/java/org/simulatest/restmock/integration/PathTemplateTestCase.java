package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

public class PathTemplateTestCase extends IntegrationTestBase {

	@Test
	public void singleSegmentCapture() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");

		assertResponseBody("/users/42", "user 42", HttpMethod.GET);
	}

	@Test
	public void multipleSegmentCaptures() throws Exception {
		RestMock.whenGet("/users/{userId}/posts/{postId}").thenReturnText("u=${userId} p=${postId}");

		assertResponseBody("/users/7/posts/99", "u=7 p=99", HttpMethod.GET);
	}

	@Test
	public void literalPathBeatsTemplateForSamePath() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");
		RestMock.whenGet("/users/me").thenReturnText("you");

		assertResponseBody("/users/me", "you", HttpMethod.GET);
		assertResponseBody("/users/42", "user 42", HttpMethod.GET);
	}

	@Test
	public void registrationOrderDoesNotMatterForSpecificity() throws Exception {
		RestMock.whenGet("/users/me").thenReturnText("you");
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");

		assertResponseBody("/users/me", "you", HttpMethod.GET);
	}

	@Test
	public void templateDoesNotSpanSlash() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("matched");

		HttpResponse<String> response = sendRequest("/users/1/extra", HttpMethod.GET);

		assertEquals(404, response.statusCode());
	}

	@Test
	public void pathCapturesCombineWithQueryParameters() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id} aka ${nickname}");

		assertResponseBody("/users/42?nickname=bob", "user 42 aka bob", HttpMethod.GET);
	}

}
