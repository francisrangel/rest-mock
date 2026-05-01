package restmock.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class StringUtilsTest {

	@Test
	public void uncapitalizeTest() {
		assertNull(StringUtils.uncapitalize(null));
		assertEquals("", StringUtils.uncapitalize(""));
		assertEquals("i am fINE", StringUtils.uncapitalize("I Am FINE"));
		assertEquals("a", StringUtils.uncapitalize("A"));
		assertEquals("uNCAPITALIZEWORD", StringUtils.uncapitalize("UNCAPITALIZEWORD"));
	}

	@Test
	public void singleLineRemovesNewlinesAndSpaces() {
		assertEquals("abc", StringUtils.singleLine("a b c"));
		assertEquals("abc", StringUtils.singleLine("a\nb\rc"));
		assertEquals("abc", StringUtils.singleLine("a\r\n b\n c"));
	}

	@Test
	public void singleLineWithNoWhitespace() {
		assertEquals("abc", StringUtils.singleLine("abc"));
	}

	@Test
	public void singleLineEmptyString() {
		assertEquals("", StringUtils.singleLine(""));
	}

}
