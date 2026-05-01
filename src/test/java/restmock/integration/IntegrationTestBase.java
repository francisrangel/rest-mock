package restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.extension.RegisterExtension;

import restmock.RestMockExtension;
import restmock.http.HttpMethod;
import restmock.utils.StringUtils;

public class IntegrationTestBase {

	protected final String baseUrl = "http://localhost:9080";

	@RegisterExtension
	static RestMockExtension server = new RestMockExtension();

	protected static final HttpClient client = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	protected void requestMethodWithResultString(String url, String expectedBody, HttpMethod method) throws Exception {
		HttpResponse<String> response = sendRequest(url, method);
		assertEquals(StringUtils.singleLine(expectedBody), StringUtils.singleLine(response.body()));
	}

	protected HttpResponse<String> sendRequest(String url, HttpMethod method) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

		switch (method) {
			case GET:     builder.GET(); break;
			case POST:    builder.POST(HttpRequest.BodyPublishers.noBody()); break;
			case PUT:     builder.PUT(HttpRequest.BodyPublishers.noBody()); break;
			case DELETE:  builder.DELETE(); break;
			case PATCH:   builder.method(HttpMethod.PATCH.name(), HttpRequest.BodyPublishers.noBody()); break;
			case HEAD:    builder.method(HttpMethod.HEAD.name(), HttpRequest.BodyPublishers.noBody()); break;
			case OPTIONS: builder.method(HttpMethod.OPTIONS.name(), HttpRequest.BodyPublishers.noBody()); break;
		}

		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

}
