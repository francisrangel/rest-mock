package org.simulatest.restmock.internal.response;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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

	/** A body that has to contain a literal ${...} doubles the dollar, the way shells do. */
	@Test
	public void aDoubledDollarIsALiteralPlaceholder() {
		Response response = new TextPlain("keep $${this}, fill ${name}");

		assertEquals("keep ${this}, fill Bob", render(response, Map.of("name", "Bob")));
	}

	@Test
	public void renderRejectsAnUnknownPlaceholder() {
		Response response = new TextPlain("Hello ${name}");

		IllegalStateException failure = assertThrows(IllegalStateException.class, () -> render(response, Map.of()));

		assertEquals("No value for ${name}. Available names: (none)", failure.getMessage());
	}

	@Test
	public void theFailureListsTheNamesThatWereAvailable() {
		Response response = new TextPlain("Hello ${nmae}");

		IllegalStateException failure =
			assertThrows(IllegalStateException.class, () -> render(response, Map.of("name", "Bob")));

		assertTrue(failure.getMessage().endsWith("Available names: name"), failure.getMessage());
	}

	@Test
	public void plainTextSubstitutesValuesUntouched() {
		Response response = new TextPlain("value=${v}");

		assertEquals("value=<a href=\"x\">", render(response, Map.of("v", "<a href=\"x\">")));
	}

	@Test
	public void jsonEscapesQuotesBackslashesAndControlCharacters() {
		Response response = new JSON("{\"v\":\"${v}\"}");

		String value = "a\"b\\c\nd";

		assertEquals("{\"v\":\"a\\\"b\\\\c\\nd\"}", render(response, Map.of("v", value)));
	}

	@Test
	public void jsonEscapesEveryControlCharacter() {
		Response response = new JSON("\"${v}\"");

		assertEquals("\"\\r\\t\\b\\f\\u0001\"", render(response, Map.of("v", "\r\t\b\f\u0001")));
	}

	/** Quotes are escaped as well as angle brackets, so a value is safe inside an attribute too. */
	@Test
	public void markupEscapesBothKindsOfQuote() {
		Response response = new Html("<a title=\"${v}\">");

		assertEquals("<a title=\"&quot;it&#39;s&quot;\">", render(response, Map.of("v", "\"it's\"")));
	}

	/** Only headers were available: no authored names to list, one header to count. */
	@Test
	public void theFailureCountsHeadersWhenNothingElseWasAvailable() {
		Response response = new TextPlain("${nope}");

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> render(response, Map.of("header.accept", "*/*")));

		assertEquals("No value for ${nope}. Available names: plus 1 request header as ${header.NAME}",
			failure.getMessage());
	}

	@Test
	public void theFailureTruncatesALongListOfNames() {
		Response response = new TextPlain("${nope}");
		Map<String, String> many = new LinkedHashMap<>();
		for (int i = 0; i < 22; i++) many.put("name" + i, "v");

		IllegalStateException failure = assertThrows(IllegalStateException.class, () -> render(response, many));

		assertTrue(failure.getMessage().endsWith("name19 and 2 more"), failure.getMessage());
	}

	@Test
	public void toStringDescribesTheBody() {
		assertEquals("hello ${name}", new TextPlain("hello ${name}").toString());
		assertEquals("<3 bytes image/png>",
			new Binary(new byte[] {1, 2, 3}, new ContentType("image/png")).toString());
	}

	@Test
	public void jsonLeavesOrdinaryValuesAlone() {
		Response response = new JSON("{\"id\":${id}}");

		assertEquals("{\"id\":42}", render(response, Map.of("id", "42")));
	}

	@Test
	public void xmlEscapesMarkupCharacters() {
		Response response = new XML("<name>${v}</name>");

		assertEquals("<name>a &amp; b &lt;c&gt;</name>", render(response, Map.of("v", "a & b <c>")));
	}

	@Test
	public void htmlEscapesMarkupCharacters() {
		Response response = new Html("<p>${v}</p>");

		assertEquals("<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>",
			render(response, Map.of("v", "<script>alert(1)</script>")));
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
		Map<String, String> header = response.getHeaders();

		response.addHeader("X-Trace", "abc");

		assertEquals("abc", header.get("X-Trace"));
	}

	@Test
	public void theHeaderViewCannotBeModifiedByCallers() {
		Map<String, String> header = new TextPlain("ok").getHeaders();

		assertThrows(UnsupportedOperationException.class, () -> header.put("X-Trace", "abc"));
	}

}
