package restmock.internal.utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonFlattener {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JsonFlattener() { }

	public static Map<String, String> flatten(String json) {
		Map<String, String> out = new LinkedHashMap<>();
		try {
			walk("", MAPPER.readTree(json), out);
		} catch (IOException malformed) {
			return new LinkedHashMap<>();
		}
		return out;
	}

	private static void walk(String prefix, JsonNode node, Map<String, String> out) {
		if (node.isObject()) {
			node.fields().forEachRemaining(e ->
				walk(prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey(), e.getValue(), out));
		} else if (node.isArray()) {
			for (int i = 0; i < node.size(); i++) {
				walk(prefix.isEmpty() ? Integer.toString(i) : prefix + "." + i, node.get(i), out);
			}
		} else if (!prefix.isEmpty()) {
			out.put(prefix, node.asText());
		}
	}

}
