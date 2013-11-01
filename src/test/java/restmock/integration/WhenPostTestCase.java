package restmock.integration;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.junit.Test;

import restmock.RestMock;
import restmock.request.HttpMethod;

public class WhenPostTestCase extends IntegrationTestBase {
	
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Test
	public void postWithoutParametersWithPlainTextResponse() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Post succeed");

		requestPostWithResultString("Post succeed");
	}

	@Test
	public void postWithOneParameter() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}!");

		requestPostWithParameters(baseUrl + "/test", "name=Bob", "Hello Bob!");
	}

	@Test
	public void postWithManyParamters() throws Exception {
		RestMock.whenPost("/test").thenReturnText("Hello ${name}! You are the number #${number} of #${total}.");

		requestPostWithParameters(baseUrl + "/test", "name=Bob&number=1&total=10", "Hello Bob! You are the number #1 of #10.");
	}

	private void requestPostWithResultString(String expectedBody) throws Exception {
		requestMethodWithResultString(baseUrl + "/test", expectedBody, HttpMethod.POST);
	}

	private void requestPostWithParameters(String url, String requestParamtersString, String resultString) throws Exception {
		ContentExchange exchange = new ContentExchange();
		exchange.setURL(url);
		exchange.setMethod(HttpMethod.POST.name());

		exchange.setRequestContent(new ByteArrayBuffer(requestParamtersString));
		exchange.setRequestContentType("application/x-www-form-urlencoded; charset=UTF-8");

		client.send(exchange);

		int exchangeState = exchange.waitForDone();

		assertEquals(HttpExchange.STATUS_COMPLETED, exchangeState);
		assertEquals(resultString + LINE_SEPARATOR, exchange.getResponseContent());
	}

}
