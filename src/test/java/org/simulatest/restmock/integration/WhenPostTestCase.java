package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class WhenPostTestCase extends IntegrationTestBase {

	@Test
	public void postWithoutParametersWithPlainTextResponse() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Post succeed");

		requestPostWithResultString("Post succeed");
	}

	@Test
	public void postWithOneParameter() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}!");

		requestPostWithParameters("/test", "name=Bob", "Hello Bob!");
	}

	@Test
	public void postWithManyParamters() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}! You are the number #${number} of #${total}.");

		requestPostWithParameters("/test", "name=Bob&number=1&total=10", "Hello Bob! You are the number #1 of #10.");
	}

	private void requestPostWithResultString(String expectedBody) throws Exception {
		assertResponseBody("/test", expectedBody, HttpMethod.POST);
	}

	private void requestPostWithParameters(String path, String requestParametersString, String resultString) throws Exception {
		HttpResponse<String> response = sendRequest(path, HttpMethod.POST,
			ContentType.APPLICATION_FORM_URLENCODED.type() + "; charset=UTF-8", requestParametersString);

		assertEquals(200, response.statusCode());
		assertEquals(resultString, response.body());
	}

}
