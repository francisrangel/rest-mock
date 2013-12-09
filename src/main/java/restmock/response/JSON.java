package restmock.response;

import java.io.Writer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

public class JSON extends Response {

	public JSON(String body) {
		super(body);
	}
	
	public JSON(Object object) {
		super(toJSON(object));
	}
	
	private static String toJSON(Object obj) {
		XStream xstream = new XStream(new JsonHierarchicalStreamDriver() {
		    public HierarchicalStreamWriter createWriter(Writer writer) {
		        return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
		    }
		});
		
		return xstream.toXML(obj).toString();
	}

	@Override
	public ContentType getContentType() {
		return ContentType.APPLICATION_JSON;
	}

}
