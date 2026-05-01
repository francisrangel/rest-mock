package restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import restmock.RestMock;
import restmock.RestMockExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeepRoutesTestCase {

	@RegisterExtension
	static RestMockExtension server = new RestMockExtension().keepRoutes();

	private static final HttpClient client = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:9080" + path))
			.GET()
			.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	@Order(1)
	public void registerRoute() throws Exception {
		RestMock.whenGet("/persistent").thenReturnText("still here");

		assertEquals(200, get("/persistent").statusCode());
	}

	@Test
	@Order(2)
	public void routeSurvivesFromPreviousTest() throws Exception {
		assertEquals(200, get("/persistent").statusCode());
	}

}
