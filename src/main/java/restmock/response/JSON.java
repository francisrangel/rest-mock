package restmock.response;

import java.io.Writer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

public final class JSON extends Response {

	private static final XStream JSON_XSTREAM = new XStream(new JsonHierarchicalStreamDriver() {
		public HierarchicalStreamWriter createWriter(Writer writer) {
			return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
		}
	});

	public JSON(String body) {
		super(body);
	}

	public JSON(Object object) {
		super(JSON_XSTREAM.toXML(object));
	}

	@Override
	public ContentType getContentType() {
		return ContentType.APPLICATION_JSON;
	}

}
