package restmock.request;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonFlattener {

	private JsonFlattener() { }

	public static Map<String, String> flatten(String json) {
		Map<String, String> out = new LinkedHashMap<>();
		try {
			Parser parser = new Parser(json);
			parser.skipWhitespace();
			parser.parseValue("", out);
			parser.skipWhitespace();
			if (parser.hasMore()) return new LinkedHashMap<>();
		} catch (RuntimeException malformed) {
			return new LinkedHashMap<>();
		}
		return out;
	}

	private static final class Parser {
		private final String src;
		private int pos;

		Parser(String src) { this.src = src; }

		boolean hasMore() { return pos < src.length(); }

		void skipWhitespace() {
			while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
		}

		void parseValue(String prefix, Map<String, String> out) {
			skipWhitespace();
			char c = src.charAt(pos);
			switch (c) {
				case '{': parseObject(prefix, out); break;
				case '[': parseArray(prefix, out); break;
				case '"': put(prefix, parseString(), out); break;
				case 't': case 'f': put(prefix, parseBoolean(), out); break;
				case 'n': parseNull(); put(prefix, "null", out); break;
				default: put(prefix, parseNumber(), out);
			}
		}

		void parseObject(String prefix, Map<String, String> out) {
			expect('{');
			skipWhitespace();
			if (peek() == '}') { pos++; return; }
			while (true) {
				skipWhitespace();
				String key = parseString();
				skipWhitespace();
				expect(':');
				parseValue(prefix.isEmpty() ? key : prefix + "." + key, out);
				skipWhitespace();
				char next = src.charAt(pos++);
				if (next == '}') return;
				if (next != ',') throw new RuntimeException("expected , or }");
			}
		}

		void parseArray(String prefix, Map<String, String> out) {
			expect('[');
			skipWhitespace();
			if (peek() == ']') { pos++; return; }
			int index = 0;
			while (true) {
				String childPrefix = prefix.isEmpty() ? Integer.toString(index) : prefix + "." + index;
				parseValue(childPrefix, out);
				skipWhitespace();
				char next = src.charAt(pos++);
				if (next == ']') return;
				if (next != ',') throw new RuntimeException("expected , or ]");
				index++;
			}
		}

		String parseString() {
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (pos < src.length()) {
				char c = src.charAt(pos++);
				if (c == '"') return sb.toString();
				if (c == '\\') {
					char esc = src.charAt(pos++);
					switch (esc) {
						case '"': sb.append('"'); break;
						case '\\': sb.append('\\'); break;
						case '/': sb.append('/'); break;
						case 'b': sb.append('\b'); break;
						case 'f': sb.append('\f'); break;
						case 'n': sb.append('\n'); break;
						case 'r': sb.append('\r'); break;
						case 't': sb.append('\t'); break;
						case 'u':
							sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
							pos += 4;
							break;
						default: throw new RuntimeException("bad escape");
					}
				} else {
					sb.append(c);
				}
			}
			throw new RuntimeException("unterminated string");
		}

		String parseNumber() {
			int start = pos;
			if (peek() == '-') pos++;
			while (pos < src.length() && "0123456789.eE+-".indexOf(src.charAt(pos)) >= 0) pos++;
			return src.substring(start, pos);
		}

		String parseBoolean() {
			if (src.startsWith("true", pos)) { pos += 4; return "true"; }
			if (src.startsWith("false", pos)) { pos += 5; return "false"; }
			throw new RuntimeException("bad boolean");
		}

		void parseNull() {
			if (!src.startsWith("null", pos)) throw new RuntimeException("bad null");
			pos += 4;
		}

		void expect(char c) {
			if (pos >= src.length() || src.charAt(pos) != c) throw new RuntimeException("expected " + c);
			pos++;
		}

		char peek() { return src.charAt(pos); }

		void put(String key, String value, Map<String, String> out) {
			if (!key.isEmpty()) out.put(key, value);
		}
	}

}
