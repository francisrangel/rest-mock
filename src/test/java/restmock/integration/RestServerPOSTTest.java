package restmock.integration;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.junit.Test;

import restmock.RestMock;
import restmock.response.TextPlain;

public class RestServerPOSTTest extends IntegrationTestBase {

	@Test
	public void postWithoutParametersWithPlainTextResponse() throws Exception {
		RestMock.whenPost("/test/").thenReturn(new TextPlain("Post succeed"));
		RestMock.startServer();

		requestPostWithResultString("Post succeed");
	}

	@Test
	public void postWithOneParameter() throws Exception {
		RestMock.whenPost("/test/").thenReturn(new TextPlain("Hello ${name}!"));
		RestMock.startServer();

		requestPostWithParameters(baseUrl + "/test/", "name=Bob", "Hello Bob!");
	}

	@Test
	public void postWithManyParamters() throws Exception {
		RestMock.whenPost("/test/").thenReturn(new TextPlain("Hello ${name}! You are the number #${number} of #${total}."));
		RestMock.startServer();

		requestPostWithParameters(baseUrl + "/test/", "name=Bob&number=1&total=10", "Hello Bob! You are the number #1 of #10.");
	}

	private void requestPostWithResultString(String expectedBody) throws Exception {
		requestMethodWithResultString(baseUrl + "/test/", expectedBody, HttpMethods.POST);
	}

	private void requestPostWithParameters(String url, String requestParamtersString, String resultString) throws Exception {
		ContentExchange exchange = new ContentExchange();
		exchange.setURL(url);
		exchange.setMethod(HttpMethods.POST);

		exchange.setRequestContent(new ByteArrayBuffer(requestParamtersString));
		exchange.setRequestContentType("application/x-www-form-urlencoded; charset=UTF-8");

		client.send(exchange);

		int exchangeState = exchange.waitForDone();

		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		assertEquals(resultString + "\r\n", exchange.getResponseContent());
	}

}
