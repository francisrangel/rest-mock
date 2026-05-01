package restmock.integration;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import restmock.RestMock;
import restmock.request.HttpMethod;
import restmock.utils.StringUtils;

public class IntegrationTestBase {

	protected final String baseUrl = "http://localhost:9080";

	protected static HttpClient client;

	@BeforeClass
	public static void setUp() {
		client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

		RestMock.startServer();
	}

	@AfterClass
	public static void cleanUp() {
		RestMock.stopServer();
	}

	@After
	public void cleanUpRoutes() {
		RestMock.clean();
	}

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
			case PATCH:   builder.method("PATCH", HttpRequest.BodyPublishers.noBody()); break;
			case HEAD:    builder.method("HEAD", HttpRequest.BodyPublishers.noBody()); break;
			case OPTIONS: builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody()); break;
		}

		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

}
