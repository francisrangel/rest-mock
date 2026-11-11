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

	/** %2F is a slash inside a segment: it belongs to the capture and does not split the path in two. */
	@Test
	public void anEncodedSlashStaysInsideItsCapture() throws Exception {
		RestMock.whenGet("/files/{name}").thenReturnText("file ${name}");

		assertResponseBody("/files/reports%2F2024.pdf", "file reports/2024.pdf", HttpMethod.GET);
		assertResponseBody("/files/a%20b+c%2fd", "file a b+c/d", HttpMethod.GET);
	}

	@Test
	public void anEncodedSlashIsShownAsSentWhenNothingMatches() throws Exception {
		RestMock.whenGet("/files/{name}").thenReturnText("file ${name}");

		HttpResponse<String> response = sendRequest("/docs/reports%2F2024.pdf", HttpMethod.GET);

		assertEquals(404, response.statusCode());
		assertEquals("No stub for GET /docs/reports%2F2024.pdf", response.body().lines().findFirst().orElseThrow());
	}

	@Test
	public void pathCapturesCombineWithQueryParameters() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id} aka ${nickname}");

		assertResponseBody("/users/42?nickname=bob", "user 42 aka bob", HttpMethod.GET);
	}

}
