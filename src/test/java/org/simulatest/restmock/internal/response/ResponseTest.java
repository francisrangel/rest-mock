package org.simulatest.restmock.internal.response;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ResponseTest {

	private String render(Response response, Map<String, String> parameters) {
		return new String(response.render(parameters), StandardCharsets.UTF_8);
	}

	@Test
	public void renderReturnsTheBodyUnchangedWhenThereAreNoPlaceholders() {
		Response response = new TextPlain("Hello World!");

		assertArrayEquals("Hello World!".getBytes(StandardCharsets.UTF_8), response.render(Map.of()));
	}

	@Test
	public void renderSubstitutesKnownPlaceholders() {
		Response response = new TextPlain("Hello ${name}, you are ${age}");

		assertEquals("Hello Bob, you are 25", render(response, Map.of("name", "Bob", "age", "25")));
	}

	@Test
	public void renderLeavesUnknownPlaceholdersLiteral() {
		Response response = new TextPlain("Hello ${name}");

		assertEquals("Hello ${name}", render(response, Map.of()));
	}

	@Test
	public void renderTreatsRegexReplacementCharactersInValuesLiterally() {
		Response response = new TextPlain("value=${v}");

		assertEquals("value=$1\\x", render(response, Map.of("v", "$1\\x")));
	}

	@Test
	public void renderEncodesNonAsciiResultsAsUtf8() {
		// built from code points, not literals: the build declares no source encoding
		String accented = new String(new char[] { 0x00e1, 0x00e4 });
		Response response = new TextPlain("name=${name}");

		assertArrayEquals(
			("name=" + accented).getBytes(StandardCharsets.UTF_8),
			response.render(Map.of("name", accented)));
	}

	@Test
	public void renderIsRepeatableForTheSameResponse() {
		Response response = new TextPlain("Hello ${name}");

		assertEquals("Hello Bob", render(response, Map.of("name", "Bob")));
		assertEquals("Hello Ada", render(response, Map.of("name", "Ada")));
	}

	@Test
	public void textResponsesAreTextual() {
		assertTrue(new TextPlain("ok").isTextual());
		assertTrue(new Html("<h1>ok</h1>").isTextual());
	}

	@Test
	public void binaryIsNotTextual() {
		Binary binary = new Binary(new byte[] {1, 2, 3}, ContentType.APPLICATION_OCTET_STREAM);

		assertFalse(binary.isTextual());
	}

	@Test
	public void binaryRenderReturnsTheBytesVerbatimEvenWhenParametersAreSupplied() {
		byte[] bytes = "${name}".getBytes(StandardCharsets.UTF_8);
		Binary binary = new Binary(bytes, ContentType.APPLICATION_OCTET_STREAM);

		assertArrayEquals(bytes, binary.render(Map.of("name", "Bob")));
	}

	@Test
	public void theHeaderViewReflectsHeadersAddedLater() {
		Response response = new TextPlain("ok");
		Map<String, String> header = response.getHeader();

		response.addHeader("X-Trace", "abc");

		assertEquals("abc", header.get("X-Trace"));
	}

	@Test
	public void theHeaderViewCannotBeModifiedByCallers() {
		Map<String, String> header = new TextPlain("ok").getHeader();

		assertThrows(UnsupportedOperationException.class, () -> header.put("X-Trace", "abc"));
	}

}
