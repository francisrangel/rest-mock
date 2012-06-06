package restmock.response;

import org.apache.commons.lang3.text.WordUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class XML extends Response {

	public XML(String body) {
		super(body);
	}
	
	public XML(Object object) {
		super(parseObjectToXML(object));		
	}

	private static String parseObjectToXML(Object object) {
		XStream parser = new XStream(new StaxDriver());
		String alias = WordUtils.uncapitalize(object.getClass().getSimpleName());
		parser.alias(alias, object.getClass());
		
		return parser.toXML(object);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_XML;
	}

}
