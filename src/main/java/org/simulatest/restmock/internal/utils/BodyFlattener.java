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
	 * {@code customer.name}. Attributes are fields of their element, and an
	 * element that has both keeps its text under its own name:
	 * {@code <price currency="USD">10</price>} yields {@code price} and
	 * {@code price.currency}. Returns an empty map if the body is not valid XML.
	 */
	public static Map<String, String> flattenXml(String xml) {
		return flatten(XML, xml);
	}

	private static Map<String, String> flatten(ObjectMapper mapper, String body) {
		Map<String, String> out = new LinkedHashMap<>();
		try {
			walk("", mapper.readTree(body), out);
		} catch (IOException malformed) {
			return Map.of();
		}
		return out;
	}

	private static void walk(String prefix, JsonNode node, Map<String, String> out) {
		if (node.isObject()) {
			// fields(), not properties(): that one arrived in Jackson 2.15, and a
			// project pinning an older 2.x would crash on every JSON request.
			node.fields().forEachRemaining(field ->
				walk(join(prefix, field.getKey()), field.getValue(), out));
		} else if (node.isArray()) {
			for (int i = 0; i < node.size(); i++) {
				walk(join(prefix, Integer.toString(i)), node.get(i), out);
			}
		} else if (!prefix.isEmpty()) {
			out.put(prefix, node.asText());
		}
	}

	/**
	 * A field with no name is how Jackson hands over the text of an XML element
	 * that also has attributes, so it is filed under the element itself.
	 */
	private static String join(String prefix, String key) {
		return prefix.isEmpty() || key.isEmpty() ? prefix + key : prefix + "." + key;
	}

}
