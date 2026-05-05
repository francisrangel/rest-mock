package restmock.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import restmock.RestMock;
import restmock.internal.http.HttpHeader;

public class BinaryFileTestCase extends IntegrationTestBase {

	@Test
	public void binaryBytesAreReturnedUntouched() throws Exception {
		byte[] bytes = {0x00, (byte) 0xFF, 0x10, (byte) 0x80, 0x7F};
		RestMock.whenGet("/binary").thenReturnFile(bytes);

		HttpResponse<byte[]> response = getBytes("/binary");

		assertEquals(200, response.statusCode());
		assertArrayEquals(bytes, response.body());
		assertEquals("application/octet-stream", contentType(response));
	}

	@Test
	public void inlineBytesWithExplicitContentType() throws Exception {
		byte[] bytes = {1, 2, 3, 4, 5};
		RestMock.whenGet("/data").thenReturnFile(bytes, "application/x-protobuf");

		HttpResponse<byte[]> response = getBytes("/data");

		assertArrayEquals(bytes, response.body());
		assertEquals("application/x-protobuf", contentType(response));
	}

	@Test
	public void resourceMimeIsInferredFromExtension() throws Exception {
		RestMock.whenGet("/page").thenReturnFileFromResource("page.html");

		HttpResponse<byte[]> response = getBytes("/page");

		assertEquals("text/html", contentType(response));
	}

	@Test
	public void resourceMimeCanBeOverridden() throws Exception {
		RestMock.whenGet("/page").thenReturnFileFromResource("page.html", "application/octet-stream");

		HttpResponse<byte[]> response = getBytes("/page");

		assertEquals("application/octet-stream", contentType(response));
	}

	@Test
	public void unknownExtensionFallsBackToOctetStream() throws Exception {
		RestMock.whenGet("/blob").thenReturnFileFromResource("fixture.xyz");

		HttpResponse<byte[]> response = getBytes("/blob");

		assertEquals("application/octet-stream", contentType(response));
	}

	@Test
	public void templateSubstitutionDoesNotApplyToBinary() throws Exception {
		byte[] bytes = "${name}".getBytes(StandardCharsets.UTF_8);
		RestMock.whenGet("/echo").thenReturnFile(bytes);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + "/echo?name=Bob"))
			.GET()
			.build();
		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

		assertArrayEquals(bytes, response.body());
	}

	@Test
	public void contentLengthMatchesByteLength() throws Exception {
		byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		RestMock.whenGet("/sized").thenReturnFile(bytes);

		HttpResponse<byte[]> response = getBytes("/sized");

		assertEquals("10", response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElseThrow());
	}

	private HttpResponse<byte[]> getBytes(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	private String contentType(HttpResponse<?> response) {
		return response.headers().firstValue(HttpHeader.CONTENT_TYPE).orElseThrow();
	}

}
