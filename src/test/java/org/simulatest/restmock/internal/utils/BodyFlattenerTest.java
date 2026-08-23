package org.simulatest.restmock.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class BodyFlattenerTest {

	@Test
	public void flatStringKey() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"name\":\"Bob\"}");
		assertEquals(Map.of("name", "Bob"), result);
	}

	@Test
	public void multipleKeys() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"a\":\"1\",\"b\":\"2\"}");
		assertEquals("1", result.get("a"));
		assertEquals("2", result.get("b"));
	}

	@Test
	public void nestedObject() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"user\":{\"name\":\"Bob\"}}");
		assertEquals(Map.of("user.name", "Bob"), result);
	}

	@Test
	public void deeplyNestedObject() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"a\":{\"b\":{\"c\":\"deep\"}}}");
		assertEquals(Map.of("a.b.c", "deep"), result);
	}

	@Test
	public void arrayWithIndices() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"items\":[{\"x\":\"a\"},{\"x\":\"b\"}]}");
		assertEquals("a", result.get("items.0.x"));
		assertEquals("b", result.get("items.1.x"));
	}

	@Test
	public void flatArray() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"tags\":[\"one\",\"two\"]}");
		assertEquals("one", result.get("tags.0"));
		assertEquals("two", result.get("tags.1"));
	}

	@Test
	public void numericValue() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"age\":25}");
		assertEquals("25", result.get("age"));
	}

	@Test
	public void negativeNumber() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"temp\":-10}");
		assertEquals("-10", result.get("temp"));
	}

	@Test
	public void floatingPoint() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"pi\":3.14}");
		assertEquals("3.14", result.get("pi"));
	}

	@Test
	public void booleanTrue() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"active\":true}");
		assertEquals("true", result.get("active"));
	}

	@Test
	public void booleanFalse() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"active\":false}");
		assertEquals("false", result.get("active"));
	}

	@Test
	public void nullValue() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"value\":null}");
		assertEquals("null", result.get("value"));
	}

	@Test
	public void escapedCharactersInString() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"msg\":\"line1\\nline2\"}");
		assertEquals("line1\nline2", result.get("msg"));
	}

	@Test
	public void unicodeEscape() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"char\":\"\\u0041\"}");
		assertEquals("A", result.get("char"));
	}

	@Test
	public void emptyObject() {
		Map<String, String> result = BodyFlattener.flattenJson("{}");
		assertTrue(result.isEmpty());
	}

	@Test
	public void emptyArray() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"items\":[]}");
		assertTrue(result.isEmpty());
	}

	@Test
	public void malformedJsonReturnsEmptyMap() {
		assertTrue(BodyFlattener.flattenJson("{not json").isEmpty());
	}

	@Test
	public void whitespaceAroundTokens() {
		Map<String, String> result = BodyFlattener.flattenJson("  { \"name\" : \"Bob\" }  ");
		assertEquals("Bob", result.get("name"));
	}

}
