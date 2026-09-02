package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.response.ContentType;

/** The one escape hatch: when the answer depends on what the request carried. */
public class AnswerTestCase extends IntegrationTestBase {

	@Test
	public void theAnswerCanDependOnTheRequest() throws Exception {
		RestMock.whenPost("/orders").thenAnswer((request, respond) -> {
			if (request.body().contains("sku")) respond.thenReturnJSON("{\"id\":1}").withStatus(201);
			else respond.thenReturnText("no sku").withStatus(400);
		});

		HttpResponse<String> created = sendRequest("/orders", HttpMethod.POST, ContentType.APPLICATION_JSON.type(), "{\"sku\":\"A1\"}");
		HttpResponse<String> rejected = sendRequest("/orders", HttpMethod.POST, ContentType.APPLICATION_JSON.type(), "{}");

		assertEquals(201, created.statusCode());
		assertEquals("{\"id\":1}", created.body());
		assertEquals(400, rejected.statusCode());
		assertEquals("no sku", rejected.body());
	}

	/** What the callback builds is an ordinary response, placeholders included. */
	@Test
	public void anAnswerIsATemplateLikeAnyOther() throws Exception {
		RestMock.whenGet("/greet").thenAnswer((request, respond) -> respond.thenReturnText("hi ${name}"));

		assertResponseBody("/greet?name=Bob", "hi Bob", HttpMethod.GET);
	}

	@Test
	public void anAnswerThatThrowsIsA500CarryingTheReason() throws Exception {
		RestMock.whenGet("/boom").thenAnswer((request, respond) -> {
			throw new IllegalStateException("kaboom");
		});

		HttpResponse<String> response = sendRequest("/boom", HttpMethod.GET);

		assertEquals(500, response.statusCode());
		assertEquals("kaboom", response.body());
	}

	@Test
	public void anAnswerThatSetsNothingIsTheUsual501() throws Exception {
		RestMock.whenGet("/silent").thenAnswer((request, respond) -> { });

		HttpResponse<String> response = sendRequest("/silent", HttpMethod.GET);

		assertEquals(501, response.statusCode());
		assertTrue(response.body().contains("no response was configured"), response.body());
	}

}
