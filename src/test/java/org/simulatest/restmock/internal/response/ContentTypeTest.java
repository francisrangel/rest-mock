package org.simulatest.restmock.internal.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ContentTypeTest {

	@Test
	public void matchesExactMediaType() {
		assertTrue(ContentType.APPLICATION_JSON.matches("application/json"));
	}

	@Test
	public void matchesIgnoringCase() {
		assertTrue(ContentType.APPLICATION_JSON.matches("APPLICATION/JSON"));
	}

	@Test
	public void matchesIgnoringParameters() {
		assertTrue(ContentType.APPLICATION_JSON.matches("application/json; charset=UTF-8"));
		assertTrue(ContentType.APPLICATION_JSON.matches("application/json;charset=UTF-8"));
		assertTrue(ContentType.APPLICATION_FORM_URLENCODED.matches("application/x-www-form-urlencoded; charset=UTF-8"));
	}

	@Test
	public void matchesIgnoringSurroundingWhitespace() {
		assertTrue(ContentType.APPLICATION_JSON.matches("  application/json  "));
	}

	@Test
	public void doesNotMatchADifferentMediaType() {
		assertFalse(ContentType.APPLICATION_JSON.matches("text/plain"));
		assertFalse(ContentType.TEXT_PLAIN.matches("text/html"));
	}

	@Test
	public void doesNotMatchAMediaTypeThatMerelySharesAPrefix() {
		assertFalse(ContentType.APPLICATION_JSON.matches("application/json-seq"));
		assertFalse(ContentType.TEXT_XML.matches("text/xml-external-parsed-entity"));
	}

	@Test
	public void doesNotMatchAMissingHeader() {
		assertFalse(ContentType.APPLICATION_JSON.matches(null));
	}

	@Test
	public void doesNotMatchAnEmptyHeader() {
		assertFalse(ContentType.APPLICATION_JSON.matches(""));
	}

	@Test
	public void guessFromInfersTheTypeFromTheExtension() {
		assertEquals(ContentType.TEXT_HTML, ContentType.guessFrom("page.html"));
	}

	@Test
	public void guessFromFallsBackToOctetStreamForAnUnknownExtension() {
		assertEquals(ContentType.APPLICATION_OCTET_STREAM, ContentType.guessFrom("fixture.xyz"));
	}

	@Test
	public void guessFromFallsBackToOctetStreamWhenThereIsNoExtension() {
		assertEquals(ContentType.APPLICATION_OCTET_STREAM, ContentType.guessFrom("payload"));
	}

}
