package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;
import org.simulatest.restmock.internal.response.ContentType;

public class BinaryFileTestCase extends IntegrationTestBase {

	/** Exact contents of src/test/resources/page.html */
	private static final byte[] PAGE_HTML_BYTES = "<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8);

	/** Exact contents of src/test/resources/fixture.xyz, trailing CRLF included */
	private static final byte[] FIXTURE_XYZ_BYTES = "binary blob\r\n".getBytes(StandardCharsets.UTF_8);

	@Test
	public void binaryBytesAreReturnedUntouched() throws Exception {
		byte[] bytes = {0x00, (byte) 0xFF, 0x10, (byte) 0x80, 0x7F};
		RestMock.whenGet("/binary").thenReturnFile(bytes);

		HttpResponse<byte[]> response = getBytes("/binary");

		assertEquals(200, response.statusCode());
		assertArrayEquals(bytes, response.body());
		assertEquals(ContentType.APPLICATION_OCTET_STREAM.type(), contentType(response));
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

		assertEquals(ContentType.TEXT_HTML.type(), contentType(response));
		assertArrayEquals(PAGE_HTML_BYTES, response.body());
	}

	@Test
	public void resourceMimeCanBeOverridden() throws Exception {
		RestMock.whenGet("/page").thenReturnFileFromResource("page.html", "application/octet-stream");

		HttpResponse<byte[]> response = getBytes("/page");

		assertEquals(ContentType.APPLICATION_OCTET_STREAM.type(), contentType(response));
		assertArrayEquals(PAGE_HTML_BYTES, response.body());
	}

	@Test
	public void unknownExtensionFallsBackToOctetStream() throws Exception {
		RestMock.whenGet("/blob").thenReturnFileFromResource("fixture.xyz");

		HttpResponse<byte[]> response = getBytes("/blob");

		assertEquals(ContentType.APPLICATION_OCTET_STREAM.type(), contentType(response));
		// the trailing CRLF proves binary resources skip the strip() that text resources get
		assertArrayEquals(FIXTURE_XYZ_BYTES, response.body());
	}

	@Test
	public void templateSubstitutionDoesNotApplyToBinary() throws Exception {
		byte[] bytes = "${name}".getBytes(StandardCharsets.UTF_8);
		RestMock.whenGet("/echo").thenReturnFile(bytes);

		HttpResponse<byte[]> response = getBytes("/echo?name=Bob");

		assertArrayEquals(bytes, response.body());
	}

	@Test
	public void contentLengthMatchesByteLength() throws Exception {
		byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		RestMock.whenGet("/sized").thenReturnFile(bytes);

		HttpResponse<byte[]> response = getBytes("/sized");

		assertEquals("10", response.headers().firstValue(HttpHeader.CONTENT_LENGTH).orElseThrow());
	}

	/** withHeader wins for binary bodies too, and nothing glues a charset onto what the caller set. */
	@Test
	public void anExplicitContentTypeHeaderOverridesTheBinaryDefault() throws Exception {
		RestMock.whenGet("/bin").thenReturnFile(new byte[] {1, 2, 3})
			.withHeader(HttpHeader.CONTENT_TYPE, "image/x-custom");

		assertEquals("image/x-custom", contentType(getBytes("/bin")));
	}

	/** Stubbing takes a copy, so a caller reusing its buffer cannot rewrite the response. */
	@Test
	public void mutatingTheCallersArrayAfterStubbingChangesNothing() throws Exception {
		byte[] buffer = "original".getBytes(StandardCharsets.UTF_8);
		RestMock.whenGet("/bin").thenReturnFile(buffer, ContentType.APPLICATION_OCTET_STREAM.type());

		Arrays.fill(buffer, (byte) 'Z');

		assertArrayEquals("original".getBytes(StandardCharsets.UTF_8), getBytes("/bin").body());
	}

}