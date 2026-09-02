package org.simulatest.restmock.integration;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

public class WhenPostTestCase extends IntegrationTestBase {

	@Test
	public void postWithoutParametersWithPlainTextResponse() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Post succeed");

		assertResponseBody("/test", "Post succeed", HttpMethod.POST);
	}

	@Test
	public void postWithOneParameter() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}!");

		postForm("name=Bob", "Hello Bob!");
	}

	@Test
	public void postWithManyParamters() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}! You are the number #${number} of #${total}.");

		postForm("name=Bob&number=1&total=10", "Hello Bob! You are the number #1 of #10.");
	}

	private void postForm(String form, String expectedBody) throws Exception {
		assertResponseBody("/test", HttpMethod.POST,
			ContentType.APPLICATION_FORM_URLENCODED.type() + "; charset=UTF-8", form, expectedBody);
	}

}
