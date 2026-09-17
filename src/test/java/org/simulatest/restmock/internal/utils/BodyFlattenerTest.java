package org.simulatest.restmock.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** The walk over the tree is what is under test; Jackson's parsing is not. */
public class BodyFlattenerTest {

	@Test
	public void objectFieldsAreNamedByKey() {
		assertEquals(Map.of("a", "1", "b", "2"), BodyFlattener.flattenJson("{\"a\":\"1\",\"b\":\"2\"}"));
	}

	@Test
	public void nestedObjectsUseDottedPaths() {
		assertEquals(Map.of("a.b.c", "deep"), BodyFlattener.flattenJson("{\"a\":{\"b\":{\"c\":\"deep\"}}}"));
	}

	@Test
	public void arrayElementsAreIndexedFromZero() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"items\":[{\"x\":\"a\"},\"b\"]}");

		assertEquals(Map.of("items.0.x", "a", "items.1", "b"), result);
	}

	@Test
	public void aRootArrayIsIndexedFromZero() {
		assertEquals(Map.of("0", "a", "1", "b"), BodyFlattener.flattenJson("[\"a\",\"b\"]"));
	}

	@Test
	public void scalarsAreRenderedAsText() {
		Map<String, String> result = BodyFlattener.flattenJson("{\"age\":25,\"pi\":3.14,\"active\":true,\"value\":null}");

		assertEquals(Map.of("age", "25", "pi", "3.14", "active", "true", "value", "null"), result);
	}

	@Test
	public void emptyContainersContributeNothing() {
		assertTrue(BodyFlattener.flattenJson("{}").isEmpty());
		assertTrue(BodyFlattener.flattenJson("{\"items\":[]}").isEmpty());
	}

	/** A body that is a bare value has no field to name, so it contributes nothing rather than failing. */
	@Test
	public void aScalarOrEmptyRootYieldsNothing() {
		assertTrue(BodyFlattener.flattenJson("42").isEmpty());
		assertTrue(BodyFlattener.flattenJson("\"hello\"").isEmpty());
		assertTrue(BodyFlattener.flattenJson("").isEmpty());
	}

	@Test
	public void malformedJsonYieldsNothing() {
		assertTrue(BodyFlattener.flattenJson("{not json").isEmpty());
	}

}
