package restmock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public class JsonFlattenerTest {

	@Test
	public void flatStringKey() {
		Map<String, String> result = JsonFlattener.flatten("{\"name\":\"Bob\"}");
		assertEquals(Map.of("name", "Bob"), result);
	}

	@Test
	public void multipleKeys() {
		Map<String, String> result = JsonFlattener.flatten("{\"a\":\"1\",\"b\":\"2\"}");
		assertEquals("1", result.get("a"));
		assertEquals("2", result.get("b"));
	}

	@Test
	public void nestedObject() {
		Map<String, String> result = JsonFlattener.flatten("{\"user\":{\"name\":\"Bob\"}}");
		assertEquals(Map.of("user.name", "Bob"), result);
	}

	@Test
	public void deeplyNestedObject() {
		Map<String, String> result = JsonFlattener.flatten("{\"a\":{\"b\":{\"c\":\"deep\"}}}");
		assertEquals(Map.of("a.b.c", "deep"), result);
	}

	@Test
	public void arrayWithIndices() {
		Map<String, String> result = JsonFlattener.flatten("{\"items\":[{\"x\":\"a\"},{\"x\":\"b\"}]}");
		assertEquals("a", result.get("items.0.x"));
		assertEquals("b", result.get("items.1.x"));
	}

	@Test
	public void flatArray() {
		Map<String, String> result = JsonFlattener.flatten("{\"tags\":[\"one\",\"two\"]}");
		assertEquals("one", result.get("tags.0"));
		assertEquals("two", result.get("tags.1"));
	}

	@Test
	public void numericValue() {
		Map<String, String> result = JsonFlattener.flatten("{\"age\":25}");
		assertEquals("25", result.get("age"));
	}

	@Test
	public void negativeNumber() {
		Map<String, String> result = JsonFlattener.flatten("{\"temp\":-10}");
		assertEquals("-10", result.get("temp"));
	}

	@Test
	public void floatingPoint() {
		Map<String, String> result = JsonFlattener.flatten("{\"pi\":3.14}");
		assertEquals("3.14", result.get("pi"));
	}

	@Test
	public void booleanTrue() {
		Map<String, String> result = JsonFlattener.flatten("{\"active\":true}");
		assertEquals("true", result.get("active"));
	}

	@Test
	public void booleanFalse() {
		Map<String, String> result = JsonFlattener.flatten("{\"active\":false}");
		assertEquals("false", result.get("active"));
	}

	@Test
	public void nullValue() {
		Map<String, String> result = JsonFlattener.flatten("{\"value\":null}");
		assertEquals("null", result.get("value"));
	}

	@Test
	public void escapedCharactersInString() {
		Map<String, String> result = JsonFlattener.flatten("{\"msg\":\"line1\\nline2\"}");
		assertEquals("line1\nline2", result.get("msg"));
	}

	@Test
	public void unicodeEscape() {
		Map<String, String> result = JsonFlattener.flatten("{\"char\":\"\\u0041\"}");
		assertEquals("A", result.get("char"));
	}

	@Test
	public void emptyObject() {
		Map<String, String> result = JsonFlattener.flatten("{}");
		assertTrue(result.isEmpty());
	}

	@Test
	public void emptyArray() {
		Map<String, String> result = JsonFlattener.flatten("{\"items\":[]}");
		assertTrue(result.isEmpty());
	}

	@Test
	public void malformedJsonReturnsEmptyMap() {
		assertTrue(JsonFlattener.flatten("{not json").isEmpty());
	}

	@Test
	public void trailingGarbageReturnsEmptyMap() {
		assertTrue(JsonFlattener.flatten("{\"a\":\"b\"} extra").isEmpty());
	}

	@Test
	public void whitespaceAroundTokens() {
		Map<String, String> result = JsonFlattener.flatten("  { \"name\" : \"Bob\" }  ");
		assertEquals("Bob", result.get("name"));
	}

}
