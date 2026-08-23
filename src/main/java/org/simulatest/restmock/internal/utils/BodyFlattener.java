package org.simulatest.restmock.internal.utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Flattens a request body into {@code dotted.path -> value} pairs, so a response
 * template can reach a nested field as {@code ${user.name}}.
 *
 * Both mappers are private and unconfigured on purpose: this is request parsing,
 * and it must not be affected by the serialization settings a test registers on
 * {@code RestMock.json()} / {@code RestMock.xml()}.
 */
public final class BodyFlattener {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final XmlMapper XML = new XmlMapper();

	private BodyFlattener() { }

	/** Flattens a JSON body. Returns an empty map if the body is not valid JSON. */
	public static Map<String, String> flattenJson(String json) {
		return flatten(JSON, json);
	}

	/**
	 * Flattens an XML body. The root element is not part of the path, so
	 * {@code <order><customer><name>Bob</name></customer></order>} yields
	 * {@code customer.name}. Returns an empty map if the body is not valid XML.
	 */
	public static Map<String, String> flattenXml(String xml) {
		return flatten(XML, xml);
	}

	private static Map<String, String> flatten(ObjectMapper mapper, String body) {
		Map<String, String> out = new LinkedHashMap<>();
		try {
			walk("", mapper.readTree(body), out);
		} catch (IOException malformed) {
			return new LinkedHashMap<>();
		}
		return out;
	}

	private static void walk(String prefix, JsonNode node, Map<String, String> out) {
		if (node == null) return;

		if (node.isObject()) {
			for (var fields = node.fields(); fields.hasNext(); ) {
				var field = fields.next();
				walk(prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey(), field.getValue(), out);
			}
		} else if (node.isArray()) {
			for (int i = 0; i < node.size(); i++) {
				walk(prefix.isEmpty() ? Integer.toString(i) : prefix + "." + i, node.get(i), out);
			}
		} else if (!prefix.isEmpty()) {
			out.put(prefix, node.asText());
		}
	}

}
